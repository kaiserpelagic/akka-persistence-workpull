package com.akka.workpull 

import scala.collection.immutable.{ Queue }
import java.util.UUID

trait Queueable {
  val id: UUID
}

object WorkState {
  def empty[T <: Queueable] = WorkState[T](
    pending = Queue.empty[T],
    inprogress = Map.empty[String, T]
  )

  trait WorkEvent
  case class WorkAddedEvt[T](work: T) extends WorkEvent
  case class WorkStartedEvt[T](work: T) extends WorkEvent
  case class WorkCompletedEvt[T](work: T) extends WorkEvent
  case class WorkCancelEvt(id: UUID) extends WorkEvent
}

case class WorkState[T <: Queueable](
  private val pending: Queue[T],
  private val inprogress: Map[String, T]) {
  import WorkState._

  def pop = pending.head

  def hasWork = pending.nonEmpty
  
  def pendingWork = pending.toList  

  def isInProgress(id: String) = inprogress.contains(id)

  def inprogressWork = inprogress.toList
  
  def update(event: WorkEvent) = event match {

    case added: WorkAddedEvt[T] => 
      copy(pending = pending.enqueue(added.work))

    case started: WorkStartedEvt[T] => {
      val (work, rest) = pending.dequeue 
      require(started.work == work, s"WorkStarted expected work ${started.work} == ${work}")
      copy(pending = rest, inprogress + (work.id.toString -> work))
    }

    case completed: WorkCompletedEvt[T] => 
      copy(inprogress = inprogress - completed.work.id.toString)

    case WorkCancelEvt(id) =>
      copy(pending = pending.filter(_.id != id))
  }
}
