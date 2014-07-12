(ns interval-training
  (:require [clojure.core.async :as a]
            [interval-training.schedule :as schedule]
            [interval-training.speaker :as speaker]
            [interval-training.ticker :as ticker]))

(defn play-schedule
  [display schedule]
  (let [control (a/chan)
        ticker (ticker/ticker-chan 1)
        process (a/go-loop [schedule schedule]
                  (a/alt! control (display "Aborted")
                          ticker ([t]
                                    (if (seq schedule)
                                      (let [[tick title] (first schedule)]
                                        (if (= tick t)
                                          (do
                                            (display title)
                                            (recur (rest schedule)))
                                          (recur schedule)))
                                      (display "Finished")))))]
    (a/go (a/<! process) (a/close! ticker))
    {:control control
     :ticker ticker
     :process process}))

(defn display
  [speaker title]
  (a/go (a/>! speaker (str title))))

(defn do-workout
  [workout]
  (let [schedule (schedule/workout-schedule workout)
        speaker (speaker/speaker-chan)
        display (partial display speaker)]
    (play-schedule display schedule)))

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
