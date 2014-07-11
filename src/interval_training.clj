(ns interval-training
  (:require [clojure.core.async :as a]
            [interval-training.schedule :as schedule]
            [interval-training.speaker :as speaker]
            [interval-training.ticker :as ticker]))

(defn do-workout
  [workout]
  (let [control (a/chan)
        speaker (speaker/speaker-chan)
        schedule (schedule/workout-schedule workout)
        ticker (ticker/ticker-chan 1)
        process (a/go-loop [schedule schedule]
                  (if (seq schedule)
                    (a/alt! control (a/>! speaker "Aborted")
                            ticker ([t]
                                      (let [[tick utterance] (first schedule)]
                                        (if (= tick t)
                                          (do
                                            (a/>! speaker (str utterance))
                                            (recur (rest schedule)))
                                          (recur schedule)))))
                    (do
                      (a/<! ticker)
                      (a/>! speaker "Finished"))))]
    (a/go (a/<! process) (a/close! ticker))
    {:control control
     :ticker ticker
     :speaker speaker
     :process process}))

(def standard-workout
  {:exercise {:names ["Jumping Jacks"
                      "Wall Sit"
                      "Push Ups"
                      "Crunches"
                      "Step Ups"
                      "Squats"
                      "Dips"
                      "Plank"
                      "Running"
                      "Lunges"
                      "Pushups with rotation"
                      "Left side plank"
                      "Right side plank"
                      "Bird Dogs"]
              :active [30 5]
              :respite [10 3]}
   :sets {:count 2
          :respite [120 10]}})

(defn -main [& args]
  (let [state (do-workout standard-workout)]
    (a/<!! (:process state))))
