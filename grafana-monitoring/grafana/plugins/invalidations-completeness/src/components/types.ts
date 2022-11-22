import { DataQuery, TimeRange } from '@grafana/data';
// import { DataSource } from './utils';

export interface PostgreSQLQuery extends DataQuery {
  format: string;
  group: string[];
  metricColumn: string;
  rawQuery: boolean;
  rawSql: string;
  refId: string;
  // select: [
  //   [
  //     {
  //       params: ['value'],
  //       type: 'column',
  //     },
  //   ],
  // ],
  timeColumn: string;
  // where: [
  //   {
  //     name: '$__timeFilter',
  //     params: [],
  //     type: 'macro',
  //   },
  // ],
  // datasource: string,
}

export interface QueryContext {
  dataSource: any;
  timeRange: TimeRange;
}
