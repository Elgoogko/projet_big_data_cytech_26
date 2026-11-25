package fr.cytech.testUtils

// Bibliothèques de sclatest
import org.scalatest.funsuite.AnyFunSuite // Importe "test"
import org.scalatest.matchers.should.Matchers // importe les mots clés "shouldBe", "in"....

// Bibliothèques de spark SQL : permet de manipulé des objets scala comme des tableau SQL
import org.apache.spark.sql.{Column, DataFrame, SparkSession}
import org.apache.spark.sql.functions._ // col, lit, when, year, month, abs...

trait TestUtils extends AnyFunSuite with Matchers {
  /**
   * Fonction de test de filtres.
   * Prend en entrée un filtre, des cas et les fais passer dans un test simple.
   *
   * @param name   Nom du filtre à tester, pour l'affichage terminal
   * @param filter Fonction filtre à tester
   * @param cases  Cas de tests sous la forme (Valeur, résultat attendu)
   * @param build  Fonction de construction des cas de test (en modifiant un cas valide)
   * @tparam T Type lambda
   */
  def checkFilter[T](name: String, filter: Column, cases: Seq[(T, Boolean)])
                    (build: T => TaxiTrip)(implicit spark: SparkSession): Unit = {
    import spark.implicits._
    for ((value, expected) <- cases) {
      test(s"$name : $value -> $expected") {
        val kept = Seq(build(value)).toDF().filter(filter).count() == 1
        kept shouldBe expected
      }
    }
  }

  /**
   * Fonction de test de filtres complexes à deux paramètres.
   * Prend en entrée un filtre, des cas à deux valeurs et les fait passer dans un test simple.
   *
   * @param name   Nom du filtre à tester, pour l'affichage terminal
   * @param filter Fonction filtre à tester
   * @param cases  Cas de tests sous la forme (Valeur 1, Valeur 2, résultat attendu)
   * @param build  Fonction de construction des cas de test (en modifiant un cas valide)
   * @tparam T1 Type de la première valeur
   * @tparam T2 Type de la deuxième valeur
   */
  def checkComplexFilters[T1, T2](name: String, filter: Column, cases: Seq[(T1, T2, Boolean)])
                                 (build: (T1, T2) => TaxiTrip)(implicit spark: SparkSession): Unit = {
    import spark.implicits._
    for ((value_1, value_2, expected) <- cases) {
      test(s"$name : $value_1; $value_2 -> $expected") {
        val kept = Seq(build(value_1, value_2)).toDF().filter(filter).count() == 1
        kept shouldBe expected
      }
    }
  }
}