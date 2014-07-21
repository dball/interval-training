(ns interval-training
  (:require [clojure.core.async :as a]
            [interval-training.gui :as gui]
            [interval-training.schedule :as schedule]
            [interval-training.speaker :as speaker]
            [interval-training.ticker :as ticker]))

(defn play-schedule
  [schedule speaker gui]
  (let [control (a/chan)
        ticker (ticker/ticker-chan 1)
        work (a/go-loop [schedule schedule
                         t 1]
               (a/alt! control "Aborted"
                       ticker (do
                                (a/>! gui t)
                                (if (seq schedule)
                                  (let [[tick title] (first schedule)
                                        t' (inc t)]
                                    (if (= tick t)
                                      (do
                                        (a/>! speaker (str title))
                                        (when (string? title)
                                          (a/>! gui title))
                                        (recur (rest schedule) t'))
                                      (recur schedule t')))
                                  "Finished"))))
        process (a/go (a/>! speaker (a/<! work))
                      (a/>! gui 0)
                      (a/>! gui "")
                      (a/close! ticker))]
    {:control control
     :ticker ticker
     :process process
     :schedule schedule}))

(defn speak
  [speaker title]
  (a/put! speaker (str title)))

(defn show
  [gui message]
  (a/put! gui message))

(defn do-workout
  [workout]
  (let [schedule (schedule/workout-schedule workout)
        speaker (speaker/speaker-chan)
        gui (gui/display-chan)]
    (play-schedule schedule speaker gui)))

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
