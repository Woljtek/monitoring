import { Checkbox } from '@grafana/ui';
import { CellProps, HeaderProps } from 'react-table';
import React from 'react';
import { ArrayVector, FieldType } from '@grafana/data';

export const SELECTION_COLUMN_WIDTH = 45;

export const getSelectorField = (size: number) => ({
  name: '_selector',
  type: FieldType.boolean,
  values: new ArrayVector(Array(size).fill(false)),
  config: { filterable: false, custom: { selector: true, align: 'center' } },
});

export const SelectorColumn = () => {
  return {
    id: '_selector',
    disableResizing: true,
    disableGroupBy: true,
    minWidth: SELECTION_COLUMN_WIDTH,
    width: SELECTION_COLUMN_WIDTH,
    maxWidth: SELECTION_COLUMN_WIDTH,
    // The header can use the table's getToggleAllRowsSelectedProps method
    // to render a checkbox
    Header: ({ getToggleAllRowsSelectedProps }: HeaderProps<any>) => <Checkbox {...getToggleAllRowsSelectedProps() as any} />,
    // The cell can use the individual row's getToggleRowSelectedProps method
    // to the render a checkbox
    Cell: ({ row }: CellProps<any>) => <Checkbox {...row.getToggleRowSelectedProps() as any} />,
  };
};
