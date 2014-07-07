(ns interval-training.core
  (:require [clojure.core.async :as a]
            [interval-training.speech :as speech]))

(defrecord Activity [title interval countdown])

(def activity ->Activity)

(defn publish-activities
  [c activities]
  (a/go-loop [[activity & activities] activities]
    (let [{:keys [title interval countdown]} activity]
      (a/>! c title)
      (a/<! (a/timeout (* 1000 (- interval countdown))))
      (loop [i countdown]
        (when (pos? i)
          (a/>! c (str i))
          (a/<! (a/timeout 1000))
          (recur (dec i)))))
    (if (seq activities)
      (recur activities)
      (a/close! c))))

(defn say-words
  [c speaker]
  (a/go-loop []
    (when-let [word (a/<! c)]
      (speech/say speaker word)
      (recur))))

(def sample-titles ["Jumping Jacks" "Wall Sit" "Push Ups"])

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

(defn do-sample-workout
  []
  (let [words (a/chan)
        rest (activity "Rest" 8 3)
        rest-between-sets (activity "Rest between sets" 10 7)
        set (build-set sample-titles 10 5 rest)
        workout (build-workout set 3 rest-between-sets)
        publishing (publish-activities words workout)]
    (say-words words speech/alex-osx-speaker)
    (a/go
      (a/<! publishing)
      (a/>! words "Finished"))))
