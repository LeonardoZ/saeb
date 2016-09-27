package controllers

import javax.inject.Inject

import models.db._
import models.entity._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future}


class StatesController @Inject()(val cityRepository: CityRepository,
                                 val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {


  def statesPage = Action.async { implicit request =>

    Future {
      Ok(views.html.states(States.all))
    }
  }

  def statePage(federativeUnit: String) = Action.async { implicit request =>
    States.getByFederativeUnit(federativeUnit) match {
      case Some(state) => generateStatePage(state)
      case None => Future {
          Ok(views.html.states(States.all))
      }
    }
  }

  def generateStatePage(state: State) = {
    val citiesF = cityRepository.getAllByState(state.federativeUnit)
    citiesF.flatMap { cities =>
      Future {
        Ok(views.html.state_page(state, Cities.citiesToSimpleCity(cities).toSeq))
      }
    }
  }



}
