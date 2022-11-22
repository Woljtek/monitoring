import React, { useCallback, useMemo, useState } from 'react';
import { ArrayVector, FieldMatcherID, InterpolateFunction, MutableDataFrame, PanelProps, TimeRange } from '@grafana/data';
import { PanelOptions } from 'types';
import { Button, HorizontalGroup, stylesFactory, TableSortByFieldState, VerticalGroup } from '@grafana/ui';
import { InvalidationEditor, Table } from 'components';
import _ from 'lodash';
import { DeleteInvalidationButton } from 'components/DeleteInvalidationButton';
import { UnlinkInvalidationButton } from 'components/UnlinkInvalidationButton';
import { css, cx } from 'emotion';
import { InvalidationLinkEditor } from 'components/InvalidationLinkEditor';
import { DataSource } from 'components/utils';

interface PanelContextProps {
  timeRange: TimeRange;
  replaceVariables: InterpolateFunction;
  dataSource: DataSource;
  options: PanelOptions;
}

export const PanelContext = React.createContext<PanelContextProps>({} as PanelContextProps);

interface Props extends PanelProps<PanelOptions> { }

export const Panel: React.FC<Props> = ({
  options,
  data,
  width,
  height,
  timeRange,
  replaceVariables,
  fieldConfig,
  onFieldConfigChange,
  onOptionsChange,
}) => {
  const styles = getStyles();
  const [selectedRowIds, setSelectedRowIds] = useState({});
  const [selectedRows, setSelectedRows] = useState<MutableDataFrame>(new MutableDataFrame());
  const [selectionLength, setSelectionLength] = useState(0);
  const [noSelection, setNoSelection] = useState(true);
  const [invalidationsAreSelected, setInvalidationAreSelected] = useState(false);
  const [invalidationEditorIsOpen, setInvalidationEditorIsOpen] = useState(false);
  const [invalidationLinkEditorIsOpen, setInvalidationLinkEditorIsOpen] = useState(false);
  const [toggleAllRowsSelected, setToggleAllRowsSelected] = useState<(value?: boolean | undefined) => void>(
    () => (value?: boolean | undefined) => { }
  );

  const postgresDatasourceName = "PostgreSQL";

  // if (postgresDatasourceName === undefined) {
  //   throw new Error("'Front PostgreSQL Datasource' is not defined in Panel options");
  // }
  const dataSource = useMemo(() => new DataSource(postgresDatasourceName), [postgresDatasourceName]);

  const getSelectedData = useCallback(
    (ids: Record<string, boolean>) => {
      // A MutableDataFrame has a get method which a DataFrame doesn't have so let's convert data
      const mutableData = new MutableDataFrame(data.series[0]);
      const newData: any = new MutableDataFrame(data.series[0]);
      newData.fields.forEach((f: any) => {
        f.values = new ArrayVector();
      });
      newData.values = {};
      _.keys(ids).forEach(id => newData.add(mutableData.get(parseInt(id, 10))));
      return newData;
    },
    [data]
  );

  const onSelectedRowIds = useCallback(
    (selectedRowIdsValues: Record<string, boolean>) => {
      setSelectedRowIds(selectedRowIdsValues);
      const selectionLength = _.keys(selectedRowIdsValues).length;
      setSelectionLength(selectionLength);
      setNoSelection(!selectionLength);
      const selectedRows = getSelectedData(selectedRowIdsValues);
      setSelectedRows(selectedRows);
      const selectedInvals = selectedRows.fields.find((f: any) => f.name === 'inval_id')?.values.toArray();
      setInvalidationAreSelected(_.some(selectedInvals, _.isNumber));
    },
    [getSelectedData]
  );

  const onColumnResize = (fieldDisplayName: string, width: number) => {
    const { overrides } = fieldConfig;

    const matcherId = FieldMatcherID.byName;
    const propId = 'custom.width';

    // look for existing override
    const override = overrides.find(o => o.matcher.id === matcherId && o.matcher.options === fieldDisplayName);

    if (override) {
      // look for existing property
      const property = override.properties.find(prop => prop.id === propId);
      if (property) {
        property.value = width;
      } else {
        override.properties.push({ id: propId, value: width });
      }
    } else {
      overrides.push({
        matcher: { id: matcherId, options: fieldDisplayName },
        properties: [{ id: propId, value: width }],
      });
    }

    onFieldConfigChange({
      ...fieldConfig,
      overrides,
    });
  };

  const onSortByChange = (sortBy: TableSortByFieldState[]) => {
    onOptionsChange({
      ...options,
      sortBy,
    });
  };

  return (
    <div className={cx(styles.wrapper)}>
      <PanelContext.Provider value={{ timeRange, replaceVariables, dataSource, options }}>
        <VerticalGroup>
          <HorizontalGroup>
            <HorizontalGroup>
              <Button
                // variant="link"
                disabled={noSelection || invalidationsAreSelected}
                icon="plus-circle"
                onClick={() => setInvalidationEditorIsOpen(true)}
              >
                Create
              </Button>
              <Button
                // variant="pen"
                disabled={selectionLength > 1 || !invalidationsAreSelected}
                icon="pen"
                onClick={() => setInvalidationEditorIsOpen(true)}
              >
                Edit
              </Button>
              <Button
                // variant="link"
                disabled={noSelection || invalidationsAreSelected}
                icon="link"
                onClick={() => setInvalidationLinkEditorIsOpen(true)}
              >
                Link
              </Button>
            </HorizontalGroup>
            <div className={cx(styles.spacer)}></div>
            <HorizontalGroup>
              <UnlinkInvalidationButton
                disabled={!invalidationsAreSelected}
                selectedRows={selectedRows}
                table="invalidation_completeness"
                toggleAllRowsSelected={toggleAllRowsSelected}
              />
              <DeleteInvalidationButton
                disabled={!invalidationsAreSelected}
                selectedRows={selectedRows}
                table="invalidation_completeness"
                toggleAllRowsSelected={toggleAllRowsSelected}
              />
            </HorizontalGroup>
          </HorizontalGroup>
          <Table
            data={data.series[0]}
            // TODO do not use fixed value
            height={height - 45} // make room for action buttons
            width={width}
            resizable={true}
            initialSortBy={options.sortBy}
            onSortByChange={onSortByChange}
            onColumnResize={onColumnResize}
            selectableRows
            selectedRowIds={selectedRowIds}
            onSelectionRowIds={onSelectedRowIds}
            toggleAllRowsSelectedCallback={setToggleAllRowsSelected}
          />
        </VerticalGroup>
        {invalidationEditorIsOpen && (
          <InvalidationEditor
            onClose={() => setInvalidationEditorIsOpen(state => !state)}
            unSelect={() => toggleAllRowsSelected(false)}
            selectedRows={selectedRows}
          ></InvalidationEditor>
        )}
        {invalidationLinkEditorIsOpen && (
          <InvalidationLinkEditor
            selectedRows={selectedRows}
            unSelect={() => toggleAllRowsSelected(false)}
            onClose={() => setInvalidationLinkEditorIsOpen(state => !state)}
          />
        )}
      </PanelContext.Provider>
    </div>
  );
};

const getStyles = stylesFactory(() => {
  return {
    wrapper: css`
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      height: 100%;
    `,
    spacer: css`
      width: 6em;
    `,
  };
});
