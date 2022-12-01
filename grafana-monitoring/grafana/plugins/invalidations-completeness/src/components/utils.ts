import { CoreApp, DataQueryRequest, DataSourceApi, dateTime, SelectableValue, TimeRange } from '@grafana/data';
import { getDataSourceSrv, getTemplateSrv } from '@grafana/runtime';
import _ from 'lodash';
import { PostgreSQLQuery, QueryContext } from './types';

export const unlinkInvalidations = async (queryContext: QueryContext, productsIds: number[]) => {
  const { dataSource, timeRange } = queryContext;
  if (productsIds.length === 0) {
    return Promise.reject('Nothing to unlink');
  }
  console.log('unlink', productsIds)
  const rawSql = `update invalidation_completeness set missing_products_ids=(SELECT array(SELECT unnest(missing_products_ids) EXCEPT SELECT unnest('{${productsIds}}'::int[])))`
  return dataSource.query(rawSql, timeRange);
};
export const removeOrphanedInvalidation = async (queryContext: QueryContext, table: string) => {
  const { dataSource, timeRange } = queryContext;
  const raw2Sql = ` DELETE FROM invalidation 
    WHERE invalidation.id NOT IN(select parent_id from invalidation_completeness WHERE parent_id=invalidation.id)`
  return dataSource.query(raw2Sql, timeRange);
};

export const findOneLinkedInvalidation = async (queryContext: QueryContext, invalidationId: number) => {
  const { dataSource, timeRange } = queryContext;
  const rawSql = `SELECT parent_id from invalidation_completeness where parent_id = ${invalidationId}`
  return dataSource.query(rawSql, timeRange);
}
export const removeOrphanedInvalidationCompleteness = async (queryContext: QueryContext, table: string) => {
  const { dataSource, timeRange } = queryContext;
  const rawSql =
    `DELETE FROM invalidation_completeness 
  WHERE missing_products_ids ='{}'
  `;
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

  const fields = data.data[0]?.fields;

  const options: any = [];
  const columnLength: number = fields[0] && fields[0].values && fields[0].values.buffer && fields[0].values.buffer.length;
  for (let index = 0; index < columnLength; index++) {
    const values: any = {}
    fields && fields.map((field: any) => {
      const key: string = field.name as string
      values[key] = field.values.buffer[index]
    })
    options.push(values)
  }
  return options;
};

// Simulate click on refresh button to refresh the table :
export const refresh = () => {
  // Get refresh div :
  const divs = document.getElementsByClassName('refresh-picker');
  // Get Button
  const button: HTMLElement = divs?.[0]?.children?.[0] as HTMLElement;
  // Simulate click
  button?.click?.();
};

