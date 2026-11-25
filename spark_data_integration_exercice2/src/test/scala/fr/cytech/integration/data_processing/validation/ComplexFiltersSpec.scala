package fr.cytech.integration.data_processing.validation

// Données de tests
import fr.cytech.fixtures._
import fr.cytech.integration.MainSpec

/**
 * Cette classe teste les filtres complexes de validation des données un par un se trouvant dans "ComplexFilters".
 * Chaque filtre est testé avec N possibilités définies dans les données de tests.
 *
 * @see fr.cytech.fixtures.ComplexDatas
 * @see fr.cytech.fixtures.SingleDatas
 */
class ComplexFiltersSpec extends MainSpec {

  // Test du filtre de date valide pour le trajet
  checkComplexFilters("test filtre dateForTripCases", ComplexFilters.validDateForTrip, ComplexDatas.dateForTripCases) {
    (v1, v2) => SingleDatas.validTrip.copy(tpep_pickup_datetime = v1, source_month = v2)
  }

  // Test du filtre de cohérence temporelle
  checkComplexFilters("test filtre temporalCoherence", ComplexFilters.validTemporalCoherence, ComplexDatas.temporalCoherenceCases) {
    (v1, v2) => SingleDatas.validTrip.copy(tpep_pickup_datetime = v1, tpep_dropoff_datetime = v2)
  }

  // Test du filtre des frais d'aéroport
  checkComplexFilters("test filtre airportFees", ComplexFilters.validAirportFees, ComplexDatas.airportFeesCases) {
    (v1, v2) => SingleDatas.validTrip.copy(PULocationID = v1, airport_fee = v2)
  }
}