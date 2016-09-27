import models.entity.{AgeGroup, City, Profile, Schooling}

package object models {

  type Group = String
  type Order = Int
  type Level = (Order, String)
  type Sex = String
  type ProfileCityGroup = (Profile, City, AgeGroup)
  type ProfileCitySchooling = (Profile, City, Schooling)
  type GroupProfiles = Map[Group, Seq[ProfileCityGroup]]
  type LevelProfiles = Map[Level, Seq[ProfileCitySchooling]]
  type SexProfilesGroup = Map[Sex, Seq[ProfileCityGroup]]
  type SexProfilesSchooling = Map[Sex, Seq[ProfileCitySchooling]]
  type SexProfilesCount = Map[Sex, Int]
  type ProfileValues = (Profile, Schooling, AgeGroup)
  type ToGroupMapper = Seq[ProfileCityGroup] => GroupProfiles

}
