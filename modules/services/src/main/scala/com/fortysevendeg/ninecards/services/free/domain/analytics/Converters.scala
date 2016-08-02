package com.fortysevendeg.ninecards.services.free.domain.analytics

import com.google.api.services.analyticsreporting.v4.{ model ⇒ Java }

object ToJavaConverters {

  import scala.collection.JavaConverters._

  implicit class JMap[A](list: List[A]) {
    def jmap[B](fun: A ⇒ B): java.util.List[B] = list.map(fun).asJava
  }

  def toGetReportsRequest(requestBody: RequestBody): Java.GetReportsRequest = {
    import requestBody._
    val requests = reportRequests.jmap(toReportRequest(viewId, dateRanges, _))
    new Java.GetReportsRequest().setReportRequests(requests)
  }

  def toReportRequest(
    viewId: String,
    dateRanges: Option[List[DateRange]],
    reportRequest: ReportRequest
  ): Java.ReportRequest = {
    import reportRequest._

    val jreq = new Java.ReportRequest() // mutable object
    jreq.setViewId(viewId)
    jreq.setDimensions(dimensions jmap toDimension)
    jreq.setMetrics(metrics jmap toMetric)
    dateRanges foreach (ranges ⇒ jreq.setDateRanges(ranges jmap toDateRange))

    jreq
  }

  def toDimension(dimension: Dimension): Java.Dimension = {
    new Java.Dimension()
      .setName(dimension.name)
      .setHistogramBuckets(dimension.histogramBuckets jmap long2Long)
  }

  def toMetric(metric: Metric): Java.Metric = {
    new Java.Metric()
      .setExpression(metric.expression)
      .setAlias(metric.alias)
      .setFormattingType(metric.formattingType.toString)
  }

  def toDateRange(dateRange: DateRange): Java.DateRange = {
    import dateRange._
    // TODO
    new Java.DateRange()
      .setStartDate("TODO")
      .setEndDate("TODO")
  }

}

object ToScalaConverters {


}

