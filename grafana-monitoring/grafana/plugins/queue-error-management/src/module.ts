import { DataSourceSettings, SelectableValue, PanelPlugin } from '@grafana/data';
import { Options } from './types';
import { Panel } from './SimplePanel';
import { getBackendSrv } from '@grafana/runtime';

export const plugin = new PanelPlugin<Options>(Panel).setPanelOptions(builder => {
  const backendSrv = getBackendSrv();
  let build = builder
    .addSelect({
      path: 'actionDatasourceName',
      name: 'Front Datasource',
      description: 'Datasource used to call actions',
      settings: {
        options: [],
      },
    })
    .addSelect({
      path: 'logAndTraceDashboard',
      name: 'Logs & Traces dashboard',
      description: 'Dashboard where to search for logs and traces',
      settings: {
        options: [],
      },
    })
    .addNumberInput({
      path: 'maximumLogAndTraceTimeRangeSpan',
      name: 'Maximum search Timerange span',
      description: 'Maximum Timerangespan of Log & Trace dashboard in seconds',
      defaultValue: 60,
    });
  // build SPRO datasource selector
  backendSrv
    .get('api/datasources')
    .then(dss =>
      dss
        .filter((e: DataSourceSettings) => e.type === 'cs-group-S1PRO')
        .map(
          (e: DataSourceSettings): SelectableValue<string> => {
            return { label: e.name, value: e.name, description: `${e.type} datasource` };
          }
        )
    )
    // FIXME: maybe a customEditor could do the trick. See example here:
    // https://github.com/marcusolsson/grafana-treemap-panel/blob/master/src/module.ts#L28-L38
    // properties is private
    //@ts-ignore
    .then(options => (build.properties[0].settings.options = options));

  //  build dashboard selector
  backendSrv
    .get('api/search', { type: 'dash-db' })
    .then(dashboards =>
      //  URL shouldn't start by "/grafana". I don't know yet if it's a bug or a feature from BackendSrv
      dashboards.map((e: any): SelectableValue<string> => ({ value: e.url.replace(/^\/grafana/, ''), label: e.title }))
    )
    .then(options => {
      //@ts-ignore
      build.properties[1].settings.options = options;
    });
  return build;
});
