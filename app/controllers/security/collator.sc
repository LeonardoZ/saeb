import java.text.Collator

val collator = Collator.getInstance
collator.setStrength(Collator.NO_DECOMPOSITION)

val w1 = Seq("Léo", "Mária", "É", "e", "Leo", "São Manuel", "Bauru", "Sao Manuel")

w1.map{ w =>
  w1.filter(w2 => collator.equals(w, w2))
}.toSet[Seq[String]].map { el =>
  if (el.size > 1)
    (el.head, el.last)
  else
    (el.head, el.head)
}
