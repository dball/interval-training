(ns interval-training.core
  (:require [clojure.core.async :as a]
            [interval-training.speech :as speech]
            [interval-training.ticker :as ticker]))

(defrecord Activity [title interval countdown])

(def activity ->Activity)

(defn add-activity
  [schedule tick activity]
  (let [{:keys [title interval countdown]} activity
        next-tick (+ tick interval)]
    (into (conj schedule [tick title])
          (map (fn [left]
                 [(- next-tick left) (str left)])
               (range countdown 0 -1)))))

(defn build-schedule
  ([activities] (build-schedule activities 1))
  ([activities tick]
     (first (reduce (fn [[schedule tick] activity]
                      [(add-activity schedule tick activity)
                       (+ tick (:interval activity))])
                    [[] tick]
                    activities))))

(defn build-set
  [titles interval countdown rest]
  (let [[a & as] (map #(activity % interval countdown) titles)
        rests (repeat rest)]
    (cons a (interleave rests as))))

(defn build-workout
  [set times rest]
  (let [[set & sets] (repeat times set)
        rests (repeat times rest)]
    (flatten (cons set (interleave rests sets)))))

(def sample-workout
  (let [exercises ["Jumping Jacks" "Wall Sit" "Push Ups"]
        rest (activity "Rest" 8 3)
        rest-between-sets (activity "Rest between sets" 10 7)
        set (build-set exercises 10 5 rest)
        workout (build-workout set 3 rest-between-sets)]
    workout))

(defn speaker-chan
  ([] (speaker-chan speech/alex-osx-speaker))
  ([speaker]
      (let [c (a/chan)]
        (a/go-loop []
                   (when-let [words (a/<! c)]
                     (speech/say speaker words)
                     (recur)))
        c)))

(defn do-workout
  [workout]
  (let [control (a/chan)
        speaker (speaker-chan)
        schedule (build-schedule workout)
        ticker (ticker/ticker-chan 1)
        process (a/go-loop [schedule schedule]
                           (if (seq schedule)
                             (let [[value chan] (a/alts! [ticker control])]
                               (if (= chan control)
                                 (do
                                   (a/>! speaker "Aborted")
                                   (a/close! ticker))
                                 (let [[tick words] (first schedule)]
                                   (if (= tick value)
                                     (do
                                       (a/>! speaker words)
                                       (recur (rest schedule)))
                                     (recur schedule)))))
                             (do
                               (a/<! ticker)
                               (a/>! speaker "Finished")
                               (a/close! ticker))))]
    {:control control
     :ticker ticker
     :speaker speaker
     :process process}))
