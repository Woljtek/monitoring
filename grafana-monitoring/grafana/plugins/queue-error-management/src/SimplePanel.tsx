import React, { PureComponent, createRef } from 'react';
import { PanelProps, MutableDataFrame, DataQueryRequest, DataQuery, CoreApp, TimeRange } from '@grafana/data';
import { Alert, Button, ConfirmModal, HorizontalGroup, IconButton } from '@grafana/ui';
import ReactTable from 'react-table-6';
import selectTableHOC from 'react-table-6/lib/hoc/selectTable/index.js';
import './css/style.css';
import 'react-table-6/react-table.css';
import { JSONViewCell } from 'components/JSONViewCell';
import { getDataSourceSrv } from '@grafana/runtime';
import { Options } from './types';
// import { FailedProcessing } from 'models';
import { ResponseDetailsModal } from 'components/ResponseDetailsModal';
import { ElementCounter } from 'components/ElementCounter';
import { countArrayElements } from 'utils';

const SelectTable = selectTableHOC(ReactTable);

// Simulate click on refresh button to refresh the table :
const refresh = () => {
  // Get refresh div :
  const divs = document.getElementsByClassName('refresh-picker-buttons');
  // Get Button
  const button: HTMLElement = divs?.[0].children?.[0] as HTMLElement;
  // Simulate click
  button?.click?.();
};

const filterCaseInsensitive = ({ id, value }: any, row: any) =>
  row[id] ? row[id].toLowerCase().includes(value.toLowerCase()) : true;
const defaultFilterMethod = (filter: any, row: any) => String(row[filter.id]).startsWith(filter.value);

const enum APIAction {
  DELETE_FAILED_PROCESSING = 'DELETE_FAILED_PROCESSING',
  RESTART_FAILED_PROCESSING = 'RESTART_FAILED_PROCESSING',
  REEVALUATE_FAILED_PROCESSING = 'REEVALUATE_FAILED_PROCESSING',
  RESUBMIT_FAILED_PROCESSING = 'RESUBMIT_FAILED_PROCESSING',

  DELETE_FAILED_PROCESSING_IN_BATCH = 'DELETE_FAILED_PROCESSING_IN_BATCH',
  RESTART_FAILED_PROCESSING_IN_BATCH = 'RESTART_FAILED_PROCESSING_IN_BATCH',
  REEVALUATE_FAILED_PROCESSING_IN_BATCH = 'REEVALUATE_FAILED_PROCESSING_IN_BATCH',
  RESUBMIT_FAILED_PROCESSING_IN_BATCH = 'RESUBMIT_FAILED_PROCESSING_IN_BATCH',
}

const enum AllowedAtions {
  RESUBMIT = 'RESUBMIT',
  RESTART = 'RESTART',
  REEVALUATE = 'REEVALUATE',
  NO_ACTION = 'NO_ACTION',
}

interface SPROQuery extends DataQuery {
  action: string;
  queryText: string;
}

const makeDataQueryRequest = (query: SPROQuery, range: TimeRange) =>
({
  requestId: query.refId,
  app: CoreApp.Dashboard,
  range: range,
  interval: '60s',
  intervalMs: 60_000,
  scopedVars: {},
  startTime: 0,
  timezone: 'utc',
  targets: [query],
} as DataQueryRequest<SPROQuery>);

class SelectedRow {
  id: number[];
  restart: boolean[];
  reevaluate: boolean[];
  resubmit: boolean[];
  constructor(id: number[] = [], restart: boolean[] = [], reevaluate: boolean[] = [], resubmit: boolean[] = []) {
    if (![restart.length, reevaluate.length, resubmit.length].every(l => l === id.length)) {
      throw new Error('SelectedRow data is not coherent');
    }
    this.id = [...id];
    this.restart = [...restart];
    this.reevaluate = [...reevaluate];
    this.resubmit = [...resubmit];
  }

  add = ({ id, allowedActions }: { id: number; allowedActions: string[] }) => {
    this.id.push(id);
    this.restart.push(allowedActions.includes(AllowedAtions.RESTART));
    this.reevaluate.push(allowedActions.includes(AllowedAtions.REEVALUATE));
    this.resubmit.push(allowedActions.includes(AllowedAtions.RESUBMIT));
  };

  remove = (id: number) => {
    const index = this.id.indexOf(id);
    this.id.splice(index, 1);
    this.restart.splice(index, 1);
    this.reevaluate.splice(index, 1);
    this.resubmit.splice(index, 1);
  };

  toggle = ({ id, allowedActions }: { id: number; allowedActions: string[] }) => {
    const index = this.id.indexOf(id);
    if (index === -1) {
      this.add({ id, allowedActions });
    } else {
      this.remove(id);
    }
  };

  length = () => this.id.length;

  includes = (id: number) => this.id.includes(id);

  commonActions = () => ({
    restart: this.restart.every(e => e),
    reevaluate: this.reevaluate.every(e => e),
    resubmit: this.resubmit.every(e => e),
  });

  copy = () => {
    return new SelectedRow(this.id, this.restart, this.reevaluate, this.resubmit);
  };
}
interface PanelState {
  selection: SelectedRow;
  selectAll: boolean;
  showConfirmModal: boolean;
  confirmModalQuery: SPROQuery | undefined;
  confirmModalBody: any;
  lastAction: string;
  idsWithSuccess: number[];
  idsSkipped: number[];
  showResponseDetails: boolean;
}

export class Panel extends PureComponent<PanelProps<Options>, PanelState> {
  checkboxTable = createRef<any>();
  keyField = 'id';

  state: Readonly<PanelState> = {
    selection: new SelectedRow(),
    selectAll: false,
    showConfirmModal: false,
    confirmModalQuery: undefined,
    confirmModalBody: {},
    lastAction: '',
    idsWithSuccess: [],
    idsSkipped: [],
    showResponseDetails: false,
  };

  componentDidUpdate(prevProps: any) {
    if (this.props.data !== prevProps.data) {
      // We want reset selection on data refresh
      this.setState({ selection: new SelectedRow(), selectAll: false });
    }
  }

  /**
   * Toggle a single checkbox for select table
   */
  toggleSelection = (key: string, shift: any, row: any) => {
    // start off with the existing state
    const selection = this.state.selection.copy();
    // key equals `select-${id}` so we use row.id instead
    selection.toggle({ id: row[this.keyField], allowedActions: row['processingDetails'].allowedActions });
    this.setState({ selection });
  };

  /**
   * Toggle all checkboxes for select table
   */
  toggleAll = () => {
    const selectAll = !this.state.selectAll;
    const selection = new SelectedRow();
    if (selectAll) {
      // we need to get at the internals of ReactTable
      const wrappedInstance = this.checkboxTable.current.getWrappedInstance();
      // the 'sortedData' property contains the currently accessible records based on the filter and sort
      const currentRecords = wrappedInstance.getResolvedState().sortedData;
      // we just push all the IDs onto the selection array
      currentRecords.forEach((item: any) => {
        selection.add({
          id: item._original[this.keyField],
          allowedActions: item._original['processingDetails'].allowedActions,
        });
      });
    }
    this.setState({ selectAll, selection });
  };

  rowFn = (state: any, rowInfo: any, column: any, instance: any) => {
    const { selection } = this.state;
    return {
      onClick: (e: any, handleOriginal: any) => {
        // IMPORTANT! React-Table uses onClick internally to trigger
        // events like expanding SubComponents and pivots.
        // By default a custom 'onClick' handler will override this functionality.
        // If you want to fire the original onClick handler, call the
        // 'handleOriginal' function.
        if (handleOriginal) {
          handleOriginal();
        }
      },
      style: {
        height: '60px',
        background: rowInfo && selection.includes(rowInfo.original.id) && 'lightgrey',
        color: rowInfo && selection.includes(rowInfo.original.id) && 'black',
      },
    };
  };

  onSearchClicked = (failedPod: string, failureDate: string) => {
    const failureTimestamp = new Date(failureDate).getTime();
    const timeSpan = this.props.options.maximumLogAndTraceTimeRangeSpan * 1000;
    const from = failureTimestamp - timeSpan;
    const to = failureTimestamp + timeSpan;
    // We want a relative URL so we remove any leading /
    // We want to do a query so we add a question mark.
    const targetUrl = this.props.options.logAndTraceDashboard.replace(/^\//, '') + '?';
    const params = new URLSearchParams(targetUrl);
    params.append('var-failedPod', encodeURIComponent(failedPod));
    params.append('var-failureDate', encodeURIComponent(failureDate));
    params.append('from', encodeURIComponent(from));
    params.append('to', encodeURIComponent(to));
    window.open(`${params.toString()}`, '_blank');
  };

  onRowDeleteClicked = (id: any) => {
    const query = { refId: 'QEM-delete', action: APIAction.DELETE_FAILED_PROCESSING, queryText: id };
    const body = ` Are you sure you want to DELETE FailedProcessing with id \`${id}\`?`;
    this.setState({
      showConfirmModal: true,
      confirmModalBody: body,
      confirmModalQuery: query,
    });
  };

  onRowCopyClicked = (failureMessage: string | undefined): void => {
    if (!failureMessage) {
      return;
    }
    navigator.clipboard.writeText(failureMessage).then(
      () => {
        console.log('copy success');
      },
      error => {
        console.log(error);
      }
    );
  };

  onRowRestartClicked = (id: any) => {
    const query = { refId: 'QEM-restart', action: APIAction.RESTART_FAILED_PROCESSING, queryText: id };
    this.doQuery(query);
  };

  onRowReevaluateClicked = (id: any) => {
    const query = { refId: 'QEM-reevaluate', action: APIAction.REEVALUATE_FAILED_PROCESSING, queryText: id };
    this.doQuery(query);
  };

  onRowResubmitClicked = (id: any) => {
    const query = { refId: 'QEM-resubmit', action: APIAction.RESUBMIT_FAILED_PROCESSING, queryText: id };
    this.doQuery(query);
  };

  onDismissConfirmModal = () => {
    this.setState({
      showConfirmModal: false,
      confirmModalQuery: undefined,
      confirmModalBody: {},
    });
  };

  onConfirmConfirmModal = () => {
    this.setState({ showConfirmModal: false });
    if (this.state.confirmModalQuery !== undefined) {
      this.doQuery(this.state.confirmModalQuery);
    }
  };

  onBatchDeleteClicked = (id: any) => {
    const query = { refId: 'QEM-delete-in-batch', action: APIAction.DELETE_FAILED_PROCESSING_IN_BATCH, queryText: id };
    const body = ' Are you sure you want to DELETE selected FailedProcessings? ';
    this.setState({
      showConfirmModal: true,
      confirmModalBody: body,
      confirmModalQuery: query,
    });
  };

  onBatchRestartClicked = (id: any) => {
    const query = {
      refId: 'QEM-restart-in-batch',
      action: APIAction.RESTART_FAILED_PROCESSING_IN_BATCH,
      queryText: id,
    };
    const body = ' Are you sure you want to RESTART selected FailedProcessings? ';
    this.setState({
      showConfirmModal: true,
      confirmModalBody: body,
      confirmModalQuery: query,
    });
  };

  onBatchReevaluateClicked = (id: any) => {
    const query = {
      refId: 'QEM-reevaluate-in-batch',
      action: APIAction.REEVALUATE_FAILED_PROCESSING_IN_BATCH,
      queryText: id,
    };
    const body = ' Are you sure you want to REEVALUATE selected FailedProcessings? ';
    this.setState({
      showConfirmModal: true,
      confirmModalBody: body,
      confirmModalQuery: query,
    });
  };

  onBatchResubmitClicked = (id: any) => {
    const query = {
      refId: 'QEM-resubmit-in-batch',
      action: APIAction.RESUBMIT_FAILED_PROCESSING_IN_BATCH,
      queryText: id,
    };
    const body = ' Are you sure you want to RESUBMIT selected FailedProcessings? ';
    this.setState({
      showConfirmModal: true,
      confirmModalBody: body,
      confirmModalQuery: query,
    });
  };

  private doQuery(query: SPROQuery) {
    const dataQueryRequest = makeDataQueryRequest(query, this.props.timeRange);
    const dataSourceSrv = getDataSourceSrv();
    dataSourceSrv
      .get(this.props.options.actionDatasourceName)
      .then(
        // FIXME: type incompatibility to remove @ts-ignore
        // @ts-ignore
        ds => ds.query(dataQueryRequest),
        (raison: any) => console.log('Query error', raison)
      )
      .then(
        (r: any) => {
          // TODO find the right type for r
          // @ts-ignore
          const { idsWithSuccess, idsSkipped } = r.data[0].values;
          this.setState({
            idsWithSuccess: idsWithSuccess.toArray().flat(),
            idsSkipped: idsSkipped.toArray().flat(),
            lastAction: query.action,
          });
          // FIXME find a way to trigger a dashboard refresh
          refresh();
        },
        (raison: any) => console.log('error log passed', raison)
      );
  }

  // Returns the columns of the  table of processing failed.
  columns() {
    return [
      {
        columns: [
          {
            Header: 'Date',
            accessor: 'failureDate',
          },
          {
            Header: 'ID',
            accessor: 'id',
            filterMethod: defaultFilterMethod,
          },
          {
            Header: 'POD',
            id: 'POD',
            accessor: (d: any) => d.failedPod,
          },
          {
            Header: 'Level',
            accessor: 'processingStatus',
          },
          {
            Header: 'Retries',
            accessor: 'nbRetries',
            filterMethod: defaultFilterMethod,
          },
        ],
      },
      {
        columns: [
          {
            Header: 'Topic Name',
            accessor: 'processingType',
          },
          {
            Header: 'Summary',
            accessor: 'summary',
            Cell: (props: any) => (
              <JSONViewCell cellValue={props.value} tooltipValue={props.original?.failureMessage} />
            ),
          },
        ],
      },
      {
        columns: [
          {
            className: 'action',
            headerClassName: 'action',
            Header: 'Actions',
            id: 'supportEngineer',
            accessor: (d: any) => {
              const multiSelection = this.state.selection.length() > 1;
              return (
                <>
                  <HorizontalGroup>
                    {/* show the row buttons if no multi selection */}
                    <IconButton
                      name="copy"
                      onClick={() => this.onRowCopyClicked(d.failureMessage)}
                      disabled={!d.failureMessage}
                      tooltip={
                        !!d.failureMessage ? 'Copy full message to clipboard' : 'Empty message : nothing to copy'
                      }
                      tooltipPlacement="bottom"
                    />
                    <IconButton
                      name="search"
                      onClick={() => d.failedPod && d.failureDate && this.onSearchClicked(d.failedPod, d.failureDate)}
                      tooltip="Open Log&Trace"
                      tooltipPlacement="bottom"
                    />
                    {!multiSelection && (
                      <HorizontalGroup>
                        <IconButton
                          name="step-backward"
                          disabled={!d.processingDetails?.allowedActions?.includes(AllowedAtions.RESTART)}
                          onClick={() => this.onRowRestartClicked(d.id)}
                          tooltip="Restart"
                          tooltipPlacement="bottom"
                        />
                        <IconButton
                          name="history"
                          onClick={() => this.onRowReevaluateClicked(d.id)}
                          disabled={!d.processingDetails?.allowedActions?.includes(AllowedAtions.REEVALUATE)}
                          tooltip="Reevaluate"
                          tooltipPlacement="bottom"
                        />
                        <IconButton
                          name="sync"
                          onClick={() => this.onRowResubmitClicked(d.id)}
                          disabled={!d.processingDetails?.allowedActions?.includes(AllowedAtions.RESUBMIT)}
                          tooltip="Resubmit"
                          tooltipPlacement="bottom"
                        />
                        <IconButton
                          name="trash-alt"
                          onClick={() => this.onRowDeleteClicked(d.id)}
                          tooltip="Delete"
                          tooltipPlacement="bottom"
                        />
                      </HorizontalGroup>
                    )}
                  </HorizontalGroup>
                </>
              );
            },
            Filter: () => {
              return this.filterCheckBox();
            },
          },
        ],
      },
    ];
  }

  // Generate buttons for multi selection instead of filter.
  filterCheckBox() {
    const display = this.state.selection.length() > 1;
    const { restart, reevaluate, resubmit } = this.state.selection.commonActions();
    return (
      <>
        {display && (
          <>
            <HorizontalGroup>
              {restart && (
                <IconButton
                  name="step-backward"
                  onClick={() => this.onBatchRestartClicked(this.state.selection.id)}
                  tooltip="Restart"
                />
              )}
              {reevaluate && (
                <IconButton
                  name="history"
                  onClick={() => this.onBatchReevaluateClicked(this.state.selection.id)}
                  tooltip="Reevaluate"
                />
              )}
              {resubmit && (
                <IconButton
                  name="sync"
                  onClick={() => this.onBatchResubmitClicked(this.state.selection.id)}
                  tooltip="Resubmit"
                />
              )}
              <IconButton
                name="trash-alt"
                onClick={() => this.onBatchDeleteClicked(this.state.selection.id)}
                tooltip="Delete"
              />
            </HorizontalGroup>
          </>
        )}
      </>
    );
  }

  render() {
    const { data } = this.props;
    const {
      showConfirmModal: showBatchConfirmModal,
      confirmModalBody: batchConfirmModalBody,
      idsWithSuccess,
      idsSkipped,
      lastAction,
      showResponseDetails,
    } = this.state;
    const dataFrame: any = new MutableDataFrame(data.series[0]);
    const dataTable = dataFrame.toArray();
    const totalLevelCounts = countArrayElements(dataFrame.values?.processingStatus?.toArray() || []);

    return (
      <>
        {!!idsWithSuccess.length && (
          <Alert
            severity="success"
            title={`${lastAction} successfully applied on ${idsWithSuccess.length} ids`}
            onRemove={() => this.setState({ idsWithSuccess: [] })}
          >
            <Button
              // variant="link"
              icon="plus"
              onClick={() => this.setState({ showResponseDetails: true })}
              style={{ color: 'white' }}
            >
              Details
            </Button>
          </Alert>
        )}
        {!!idsSkipped.length && (
          <Alert
            severity="error"
            title={`${lastAction} failed on ${idsSkipped.length} ids`}
            onRemove={() => this.setState({ idsSkipped: [] })}
          >
            <Button
              // variant="link"
              icon="plus"
              onClick={() => this.setState({ showResponseDetails: true })}
              style={{ color: 'white' }}
            >
              Details
            </Button>
          </Alert>
        )}
        {
          <ResponseDetailsModal
            isOpen={showResponseDetails}
            onDismiss={() => this.setState({ showResponseDetails: false })}
            action={lastAction}
            idsWithSuccess={idsWithSuccess}
            idsSkipped={idsSkipped}
          />
        }
        <SelectTable
          ref={this.checkboxTable}
          selectType="checkbox"
          keyField={this.keyField}
          isSelected={(id: number) => this.state.selection.includes(id)}
          selectAll={this.state.selectAll}
          toggleAll={this.toggleAll}
          toggleSelection={this.toggleSelection}
          getTdProps={this.rowFn}
          defaultPageSize={10}
          filterable
          defaultFilterMethod={filterCaseInsensitive}
          data={dataTable}
          className="-striped -highlight"
          noDataText="No data found"
          columns={this.columns()}
        >
          {(state: any, makeTable: any, instance: any) => {
            const { sortedData } = state;
            const filteredCount =
              sortedData?.length > 0 ? countArrayElements(sortedData.map((e: any) => e['processingStatus'])) : {};

            return (
              <>
                <ElementCounter filteredCount={filteredCount} totalCount={totalLevelCounts} />
                {makeTable()}
              </>
            );
          }}
        </SelectTable>
        <ConfirmModal
          isOpen={showBatchConfirmModal}
          title="Do batch action"
          confirmText="Confirm"
          onConfirm={this.onConfirmConfirmModal}
          onDismiss={this.onDismissConfirmModal}
          body={batchConfirmModalBody}
        />
      </>
    );
  }
}
