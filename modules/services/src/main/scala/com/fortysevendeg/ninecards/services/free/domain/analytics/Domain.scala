package com.fortysevendeg.ninecards.services.free.domain.analytics

import org.joda.time.DateTime

/*First, we do the bad encoding, only later we do the good one. */

/* Enums */

object FilterLogicalOperator extends Enumeration {
  type FilterLogicalOperator = Value
  val AND, OR, OPERATOR_UNSPECIFIED = Value
}

object MetricType extends Enumeration {
  type MetricType = Value
  val METRIC_TYPE_UNSPECIFIED, INTEGER, FLOAT, CURRENCY, PERCENT, TIME = Value
}

object ComparisonOperator extends Enumeration {
  type Operator = Value
  val OPERATOR_UNSPECIFIED, EQUAL, LESS_THAN, GREATER_THAN, IS_MISSING = Value
}

object OrderType extends Enumeration {
  type OrderType = Value
  val ORDER_TYPE_UNSPECIFIED, VALUE, DELTA, SMART, HISTOGRAM_BUCKET, DIMENSION_AS_INTEGER = Value
}

object SortOrder extends Enumeration {
  type SortOrder = Value
  val SORT_ORDER_UNSPECIFIED, ASCENDING, DESCENDING = Value
}

/* A scope for a metric defines the level at which that metric is
 *  defined - PRODUCT, HIT, SESSION, or USER. Metric values can also be
 * reported at scopes greater than its primary scope.
 *
 * E.g., ga:pageviews and ga:transactions can be reported at SESSION and
 * USER level by just adding them up for each hit that occurs in those
 * sessions or for those users.
 */
object Scope extends Enumeration {
  type Scope = Value
  val PRODUCT, HIT, SESSION, USER, UNSPECIFIED_SCOPE = Value
}

/***** The Case Classes, in a Top-Down Approach, first Requests and then Responses ***/

import FilterLogicalOperator._
import MetricType._
import Scope._
import OrderType._
import SortOrder._

/*
 *  Requests, each request will have a separate response.  There can be
 *  a maximum of 5 requests.  All requests should have the same
 *  dateRanges, viewId, segments, samplingLevel, and cohortGroup.
 */
case class RequestBody(
  /* All requests should have the same
   * dateRanges, viewId, segments, samplingLevel, and cohortGroup.
   */
  viewId: String,
  /* If a date range is not provided, the default is (startDate:
   * current date - 7 days, endDate: current date - 1 day). */
  dateRanges: Option[List[DateRange]],
  reportRequests: List[ReportRequest]
)

/* Interesting requirements:
 * - If there is a cohort group in the request the ga:cohort dimension must be present. 
 * - Requests with segments must have the ga:segment dimension
 * 
 */

case class ReportRequest(
  /* The dimensions requested. Requests can have a total of 7 dimensions.
   */
  dimensions: List[Dimension],

  /* The metrics requested. Requests must specify at least one metric.
   * Requests can have a total of 10 metrics.
   */
  metrics: List[Metric],

  /* Sort order on output rows. To compare two rows, the elements of
   * the following are applied in order until a difference is
   * found. All date ranges in the output get the same row order.
   */
  orderBys: List[OrderBy]
)

/*
 A contiguous set of days: startDate, startDate + 1 day, ..., endDate.
 The start and end dates are specified in ISO8601 date format YYYY-MM-DD.
 */
case class DateRange(
  startDate: DateTime,
  endDate: DateTime
)

/*
 Dimensions are attributes of your data. For example, the dimension ga:city
 indicates the city, for example, "Paris" or "New York", from which a session originates.
 */
case class Dimension(
  name: String,
  histogramBuckets: List[Long]
)

/* Metrics are the quantitative measurements. For example, the metric ga:users
 * indicates the total number of users for the requested time period. */
case class Metric(
  expression: String,
  alias: String,
  formattingType: MetricType
)

case class OrderBy(
  fieldName: String,
  orderType: OrderType,
  sortOrder: SortOrder
)

/***** Responses ****/

case class ResponseBody(reports: List[Report])

case class Report(
  columnHeader: ColumnHeader,
  data: ReportData,
  nextPageToken: String
)

case class ColumnHeader(
  dimensions: List[String],
  metricHeader: MetricHeader
)

case class MetricHeader(
  metricHeaderEntries: List[MetricHeaderEntry],
  pivotHeaders: List[PivotHeader]
)

case class MetricHeaderEntry(
  name: String,
  type_t: MetricType // NOTE: This must be serialised to "type" */
)

case class PivotHeader(
  pivotHeaderEntries: List[PivotHeaderEntry],
  totalPivotGroupsCount: Int
)

case class PivotHeaderEntry(
  dimensionNames: List[String],
  dimensionValues: List[String],
  metric: MetricHeaderEntry
)

case class ReportData(
  rows: List[ReportRow],
  totals: List[DateRangeValues],
  rowCount: Integer,
  minimums: List[DateRangeValues],
  maximums: List[DateRangeValues],
  samplesReadCounts: List[String] // serialised as an int64 format.

)

case class ReportRow(
  dimensions: List[String],
  metrics: List[DateRangeValues]
)

case class DateRangeValues(
  values: List[String]
)

