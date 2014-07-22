(ns interval-training
  (:require [clojure.core.async :as a]
            [interval-training.gui :as gui]
            [interval-training.schedule :as schedule]
            [interval-training.speaker :as speaker]
            [interval-training.ticker :as ticker]))

(defn next-activities
  [[activity & activities]]
  (let [{:keys [duration countdown]} activity]
    (if (> duration 1)
      (cons (-> activity
                (update-in [:duration] dec)
                (assoc :ongoing? true)
                (cond-> (= (dec duration) countdown)
                        (assoc :counting-down? true)))
            activities)
      activities)))

(defn play-activities
  [activities ui]
  (let [control (a/chan)
        ticker (ticker/ticker-chan 1)
        {:keys [events speaker display]} ui
        work (a/go-loop [state {:activities activities
                                :active? true}]
               (let [{:keys [activities active?]} state
                     [val task] (a/alts! [control ticker])]
                 (condp = task
                   control
                   (if val
                     (let [action (if active? :pause :continue)]
                       (a/>! speaker (name action))
                       (recur (update-in state [:active?] not)))
                     :aborted)

                   ticker
                   (if active?
                     (when-let [{:keys [title
                                        duration
                                        counting-down?
                                        ongoing?]} (first activities)]
                       (when-not ongoing?
                         (a/>! display title)
                         (a/>! speaker title))
                       (a/>! display duration)
                       (when counting-down?
                         (a/>! speaker (str duration)))
                       (recur (update-in state [:activities]
                                         next-activities)))
                     (recur state)))))
        process (a/go (a/>! speaker (name (a/<! work)))
                      (a/>! display 0)
                      (a/>! display "")
                      (a/close! ticker))]
    (a/go-loop []
      (when-let [val (a/<! events)]
        (a/>! control val)
        (recur)))
    {:control control
     :process process}))

(defn do-workout
  [workout]
  (let [activities (schedule/workout-activities workout)
        ui (assoc (gui/display-channels)
             :speaker (speaker/speaker-chan))]
    (play-activities activities ui)))

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
              :respite [9 3]}
   :sets {:count 2
          :respite [120 10]}})

(defn -main [& args]
  (let [state (do-workout standard-workout)]
    (a/<!! (:process state))))
