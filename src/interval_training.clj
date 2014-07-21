(ns interval-training
  (:require [clojure.core.async :as a]
            [interval-training.gui :as gui]
            [interval-training.schedule :as schedule]
            [interval-training.speaker :as speaker]
            [interval-training.ticker :as ticker]))

(defn play-schedule
  [schedule ui]
  (let [control (a/chan 4)
        ticker (ticker/ticker-chan 1)
        {:keys [events speaker display]} ui
        work (a/go-loop [schedule schedule
                         tasks #{control ticker events}
                         t 1]
               (let [[val task] (a/alts! (vec tasks))]
                 (condp = task
                   control
                   (if val
                     (let [running? (tasks ticker)
                           tasks ((if running? disj conj) tasks ticker)]
                       (a/>! speaker (if running? "Pause" "Continue"))
                       (recur schedule tasks t))
                     :aborted)

                   events
                   (do
                     (a/>! control val)
                     (recur schedule tasks t))

                   ticker
                   (do
                     (a/>! display t)
                     (if (seq schedule)
                       (let [[tick title] (first schedule)
                             t' (inc t)]
                         (if (= tick t)
                           (do
                             (a/>! speaker (str title))
                             (when (string? title)
                               (a/>! display title))
                             (recur (rest schedule) tasks t'))
                           (recur schedule tasks t')))
                       :finished)))))
        process (a/go (a/>! speaker (name (a/<! work)))
                      (a/>! display 0)
                      (a/>! display "")
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
        ui (assoc (gui/display-channels)
             :speaker (speaker/speaker-chan))]
    (play-schedule schedule ui)))

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
