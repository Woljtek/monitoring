import React, { useState } from 'react';
import { StandardEditorProps, DataSourceSettings, SelectableValue } from '@grafana/data';
import { Select } from '@grafana/ui';
import { getBackendSrv } from '@grafana/runtime';

interface Settings {
  datasourceType: 'postgres';
}

interface Props extends StandardEditorProps<string | string[], Settings> { }

export const DatasourceSelectEditor: React.FC<Props> = ({ item, value, onChange }) => {
  const [options, setOptions] = useState([]);
  getBackendSrv()
    .get('api/datasources')
    .then(dss =>
      dss
        .filter((e: DataSourceSettings) => e.type === item.settings?.datasourceType ?? 'postgres')
        .map(
          (e: DataSourceSettings): SelectableValue<string> => {
            return { label: e.name, value: e.name, description: `${e.type} datasource` };
          }
        )
    )
    .then(options => setOptions(options));
  return <Select value={value} onChange={(e: any) => onChange(e.value)} options={options} />;
};
