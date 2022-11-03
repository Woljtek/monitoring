type SeriesSize = 'sm' | 'md' | 'lg';

export interface SimpleOptions {
  text: string;
  showSeriesCount: boolean;
  seriesCountSize: SeriesSize;
}
export interface Options {
  actionDatasourceName: string;
  logAndTraceDashboard: string;
  maximumLogAndTraceTimeRangeSpan: number;
}
