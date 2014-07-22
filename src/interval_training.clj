(ns interval-training
  (:require [clojure.core.async :as a]
            [interval-training.gui :as gui]
            [interval-training.schedule :as schedule]
            [interval-training.speaker :as speaker]
            [interval-training.ticker :as ticker]))

(defn play-schedule
  [schedule ui]
  (let [control (a/chan)
        ticker (ticker/ticker-chan 1)
        {:keys [events speaker display]} ui
        tasks [control ticker]
        work (a/go-loop [state {:schedule schedule
                                :running? true
                                :t 1}]
               (let [{:keys [schedule running? t]} state
                     [val task] (a/alts! (vec tasks))]
                 (condp = task
                   control
                   (if val
                     (do
                       (a/>! speaker (if running? "Pause" "Continue"))
                       (when (not running?) (a/<! ticker))
                       (recur (assoc state :running? (not running?))))
                     :aborted)

                   ticker
                   (if running?
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
                               (recur (assoc state :schedule (rest schedule) :t t')))
                             (recur (assoc state :t t'))))
                         :finished))
                     (recur state)))))]
    (a/go-loop []
      (when-let [val (a/<! events)]
        (a/>! control val)
        (recur)))
    (a/go (a/>! speaker (name (a/<! work)))
          (a/>! display 0)
          (a/>! display "")
          (a/close! ticker))
    {:control control}))

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
              :active [10 5]
              :respite [5 3]}
   :sets {:count 2
          :respite [120 10]}})

(defn -main [& args]
  (let [state (do-workout standard-workout)]
    (a/<!! (:process state))))
