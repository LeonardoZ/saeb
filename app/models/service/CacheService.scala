package models.service

import javax.inject.Inject

import models.db.{AgeGroupRepository, CityRepository, SchoolingRepository}
import play.api.cache.CacheApi
import play.cache.NamedCache


class CacheService @Inject()(@NamedCache("db-cache")  val cacheApi: CacheApi,
                             val cityRepository: CityRepository,
                             val schoolingRepository: SchoolingRepository,
                             val ageGroupRepository: AgeGroupRepository) {
//
//  def persistAndGet(city: City): Future[Option[City]] = {
//    cacheApi.getOrElse(city.name)(cityRepository.insertReturningId(city).flatMap({
//      val x = cityRepository.getById(_)
//      cacheApi.set(city.name, city, 5 minutes)
//      x
//    }))
//  }
//
//  def persistAndGet(schooling: Schooling): Future[Option[Schooling]] = {
//    cacheApi.getOrElse(schooling.level)(schoolingRepository.insertReturningId(schooling).flatMap({
//      val x = schoolingRepository.getById(_)
//      cacheApi.set(schooling.level, schooling, 5 minutes)
//      x
//    }))
//  }
//
//  def persistAndGet(age: AgeGroup): Future[Option[AgeGroup]] = {
//    cacheApi.getOrElse(age.group)(ageGroupRepository.insertReturningId(age).flatMap({
//      val x = ageGroupRepository.getById(_)
//      cacheApi.set(age.group, age, 5 minutes)
//      x
//    }))
//  }

}
