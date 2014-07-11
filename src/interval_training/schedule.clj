(ns interval-training.schedule)

(defn build-activity
  [title [duration countdown]]
  {:title title
   :duration duration
   :countdown countdown})

(defn interleave-respites
  [respite things]
  (if (seq things)
    (flatten (cons (first things) (interleave (repeat respite) (rest things))))
    things))

(defn build-activities
  [workout]
  (let [{:keys [exercise sets]} workout
        respite (build-activity "Respite" (:respite exercise))
        respite-between-sets (build-activity "Respite between sets" (:respite sets))
        exercises (map #(build-activity % (:active exercise)) (:names exercise))
        set (interleave-respites respite exercises)]
    (interleave-respites respite-between-sets (repeat (:count sets) set))))

(defn build-cue
  [tick title]
  [tick title])

(defn add-activity
  [schedule tick activity]
  (let [{:keys [title duration countdown]} activity
        next-tick (+ tick duration)]
    (into (conj schedule (build-cue tick title))
          (map (fn [left]
                 (build-cue (- next-tick left) left))
               (range countdown 0 -1)))))

(defn build-schedule
  [activities]
  (first (reduce (fn [[schedule tick] activity]
                   [(add-activity schedule tick activity)
                    (+ tick (:duration activity))])
                 [[] 1]
                 activities)))

(defn workout-schedule
  [workout]
  (-> workout
      build-activities
      build-schedule))
