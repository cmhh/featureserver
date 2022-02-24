package org.cmhh.featureserver

/**
 * Simple class to represent a result that might fail, or be empty.
 */
sealed abstract class Result[+A]
case class NonEmpty[+A](a: A) extends Result[A]
case object Empty extends Result[Nothing]
case class Error[+A](throwable: Throwable) extends Result[A]