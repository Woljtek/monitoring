import React from 'react';
import { CellProps, HeaderProps, Hooks } from 'react-table';
import { Checkbox } from '@grafana/ui';
import { SELECTION_COLUMN_WIDTH } from './SelectorColumn';

export const selectionHook = (hooks: Hooks<any>) => {
  hooks.allColumns.push(columns => [
    // Let's make a column for selection
    {
      id: '_selector',
      disableResizing: true,
      disableGroupBy: true,
      minWidth: SELECTION_COLUMN_WIDTH,
      width: SELECTION_COLUMN_WIDTH,
      maxWidth: SELECTION_COLUMN_WIDTH,
      // The header can use the table's getToggleAllRowsSelectedProps method
      // to render a checkbox
      Header: ({ getToggleAllRowsSelectedProps }: HeaderProps<any>) => (
        <Checkbox {...getToggleAllRowsSelectedProps() as any} />
      ),
      // The cell can use the individual row's getToggleRowSelectedProps method
      // to the render a checkbox
      Cell: ({ row }: CellProps<any>) => <Checkbox {...row.getToggleRowSelectedProps() as any} />,
    },
    ...columns,
  ]);
};
