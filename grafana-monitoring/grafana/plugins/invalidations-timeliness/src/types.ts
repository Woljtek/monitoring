import { TableSortByFieldState } from '@grafana/ui';
import { RootCauseEditorOptions } from 'components/RootCauseEditor/types';


export interface PanelOptions {
  postgresDatasourceName: string;
  frameIndex: number;
  sortBy?: TableSortByFieldState[];
  rootCauseListTimeliness: RootCauseEditorOptions[];
  // rootCauseListCompleteness: RootCauseEditorOptions[];
}

export interface TableSortBy {
  displayName: string;
  desc: boolean;
}

export interface CustomFieldConfig {
  width: number;
  displayMode: string;
}
