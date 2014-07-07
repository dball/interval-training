(ns interval-training.speech
  (:require [clojure.java.shell :refer [sh]])
  (:import
   (com.sun.speech.freetts Voice)
   (com.sun.speech.freetts VoiceManager)
   (com.sun.speech.freetts.audio JavaClipAudioPlayer)
   (com.sun.speech.freetts.audio JavaStreamingAudioPlayer)
   (javax.sound.sampled AudioFileFormat$Type)))

(defprotocol Speaker
  (say [_ word]))

(defrecord FreeTTSSpeaker [dir voice]
  Speaker
  (say [_ word]
    (System/setProperty "freetts.voices" dir)
    (let [voice-manager (. VoiceManager getInstance)
          audioplayer (doto (new JavaStreamingAudioPlayer))]
      (doto (. voice-manager getVoice voice)
        (.allocate)
        (.setAudioPlayer audioplayer)
        (.speak word)
        (.deallocate))
      (.close audioplayer))
    nil))

(def kevin-speaker
  (->FreeTTSSpeaker "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory"
                    "kevin16"))

(def osx-voices
  {:agnes "Agnes"
   :kathy "Kathy"
   :princess "Princess"
   :vicki "Vicki"
   :victoria "Victoria"

   :bruce "Bruce"
   :fred "Fred"
   :junior "Junior"
   :ralph "Ralph"
   :alex "Alex"                     ; probably most realistic sounding

   :albert "Albert"
   :zarvox "Zarvox"
   :trinoids "Trinoids"
   :whisper "Whisper"

   :bahh "Bahh"
   :boing "Boing"
   :bubbles "Bubbles"
   :bad-news "Bad News"
   :good-news "Good News"
   :deranged "Deranged"
   :hysterical "Hysterical"

   :bells "Bells"
   :cellos "Cellos"
   :pipe-organ "Pipe Organ"})

(defrecord OSXSpeaker [voice]
  Speaker
  (say [_ word]
    (let [osx-voice (get osx-voices voice voice)]
      (sh "say" "-v" osx-voice word))
    nil))

(def alex-osx-speaker
  (->OSXSpeaker :alex))
