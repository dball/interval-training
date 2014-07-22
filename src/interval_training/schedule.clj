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

(defn workout-activities
  [workout]
  (let [{:keys [exercise sets]} workout
        respite (build-activity "Rest" (:respite exercise))
        respite-between-sets (build-activity "Rest between sets" (:respite sets))
        exercises (map #(build-activity % (:active exercise)) (:names exercise))
        set (interleave-respites respite exercises)]
    (interleave-respites respite-between-sets (repeat (:count sets) set))))
