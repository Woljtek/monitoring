import { CoreApp, DataQueryRequest, DataSourceApi, dateTime, SelectableValue, TimeRange } from '@grafana/data';
import { getDataSourceSrv, getTemplateSrv } from '@grafana/runtime';
import _ from 'lodash';
import { PostgreSQLQuery, QueryContext } from './types';

export const unlinkInvalidations = async (queryContext: QueryContext, datatakeDbIds: number[]) => {
  const { dataSource, timeRange } = queryContext;
  if (datatakeDbIds.length === 0) {
    return Promise.reject('Nothing to unlink');
  }
  const table = 'planned_datatake';
  const datatakeDbIdsString = datatakeDbIds.join(',');
  const rawSql = `update ${table} set invalidation_id = null where datatake_id IN(${datatakeDbIdsString})`;
  return dataSource.query(rawSql, timeRange);
};

export const removeOrphanedInvalidation = async (queryContext: QueryContext, table: string) => {
  const { dataSource, timeRange } = queryContext;
  const rawSql = `DELETE FROM invalidation WHERE origin_table = '${table}' AND id not in(SELECT invalidation_id FROM ${table} JOIN invalidation ON ${table}.invalidation_id = invalidation.id)`;
  return dataSource.query(rawSql, timeRange);
};

export class DataSource {
  name: string;
  datasource: Promise<DataSourceApi>;
  constructor(name: string) {
    this.name = name;
    this.datasource = getDataSourceSrv().get(name);
  }

  async query(rawSql: string, timeRange?: TimeRange) {
    const usedTimeRange = timeRange ?? getCurrentTimeRange();
    return this.datasource.then(
      // FIXME: type incompatibility to remove @ts-ignore:
      // Property 'toPromise' does not exists on type 'Promise<DataQueryResponse> | Observable<DataQueryResponse>'.
      // Property 'toPromise' does not exists on type 'Promise<DataQueryResponse>.
      // @ts-ignore
      ps => ps.query(this.buildQueryOptions(usedTimeRange, rawSql)).toPromise(),
      raison => console.log('Query Error', raison)
    );
  }

  buildQueryOptions(range: TimeRange, rawSql: string): DataQueryRequest<PostgreSQLQuery> {
    return {
      requestId: `${this.name}Query`,
      app: CoreApp.Dashboard,
      range: range,
      targets: [
        {
          format: 'table',
          group: [],
          metricColumn: 'none',
          rawQuery: true,
          rawSql: rawSql,
          refId: 'A',
          // select: [
          //   [
          //     {
          //       params: ['value'],
          //       type: 'column',
          //     },
          //   ],
          // ],
          timeColumn: 'time',
          // where: [
          //   {
          //     name: '$__timeFilter',
          //     params: [],
          //     type: 'macro',
          //   },
          // ],
          datasource: 'PostgreSQL',
        },
      ],
      scopedVars: {},
      timezone: 'browser',
      panelId: 1,
      dashboardId: 1,
      interval: '60s',
      intervalMs: 60000,
      maxDataPoints: 500,
      startTime: 0,
    } as DataQueryRequest<PostgreSQLQuery>;
  }
}

export const getCurrentTimeRange = () => {
  const templateSrv = getTemplateSrv();
  const from = dateTime(templateSrv.replace('${__from:date}'));
  const to = dateTime(templateSrv.replace('${__to:date}'));
  return { from, to, raw: { from, to } };
};

// Convert raw data from database to object usable by Select component (assuming that data is composed of this tree columns: value, label and description)
export const buildOptionsFromData = (data: any): Array<SelectableValue<number>> => {
  const { rows, columns } = data.data[0];
  const columnArray = columns.map((c: { text: any }) => c.text);
  return rows.map((row: any[]) => _.zipObject(columnArray, row));
};

// Simulate click on refresh button to refresh the table :
export const refresh = () => {
  // Get refresh div :
  const divs = document.getElementsByClassName('refresh-picker-buttons');
  // Get Button
  const button: HTMLElement = divs?.[0].children?.[0] as HTMLElement;
  // Simulate click
  button?.click?.();
};
