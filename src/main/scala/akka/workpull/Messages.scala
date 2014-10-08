package com.akka.workpull 

import akka.actor.ActorRef

object PullingPattern {
  sealed trait Message

  case object GimmeWork extends Message
  case object NoWorkToBeDone extends Message
  case object WorkAvailable extends Message

  case class RegisterWorker(worker: ActorRef) extends Message

  case class Work[T](work: T) extends Message
  case class WorkCompleted[T](work: T) extends Message
  case class WorkFailed[T](work: T) extends Message
  case class WorkFinished[T](work: T) extends Message

  case class CancelJob(id: java.util.UUID) extends Message 
}
