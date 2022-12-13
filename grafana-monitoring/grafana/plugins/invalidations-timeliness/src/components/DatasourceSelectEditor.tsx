import React, { useEffect } from 'react';
import { StandardEditorProps, DataSourceSettings, SelectableValue } from '@grafana/data';
import { Select } from '@grafana/ui';
import { getBackendSrv } from '@grafana/runtime';

export const optionsDataSource: Array<SelectableValue<string>> = []
interface Settings {
  datasourceType: 'postgres';
}

interface Props extends StandardEditorProps<string | string[], Settings> { }

export const DatasourceSelectEditor: React.FC<Props> = ({ item, value, onChange }) => {
  useEffect(() => {
    getBackendSrv()
      .get('api/datasources')
      .then(dss => {
        dss
          .filter((e: DataSourceSettings) => e.type === item.settings?.datasourceType ?? 'postgres')
          .map(
            (e: DataSourceSettings) => {
              optionsDataSource.push({ label: e.name + ' ' + e.database, value: e.name + ' ' + e.database, description: `${e.type} datasource` });
            }
          )
      }
      )
  }, [item.settings?.datasourceType])


  return optionsDataSource.length > 0 ? <Select defaultValue={optionsDataSource[0].value} value={value} onChange={(e: any) => onChange(e.value)} options={optionsDataSource} /> : null;
};
