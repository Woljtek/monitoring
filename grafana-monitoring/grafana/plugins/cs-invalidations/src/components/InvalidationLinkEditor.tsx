import React, { FC, useContext, useReducer, useState } from 'react';
import { MutableDataFrame, SelectableValue, TimeRange } from '@grafana/data';
import {
  Button,
  CollapsableSection,
  Drawer,
  Field,
  Form,
  HorizontalGroup,
  // Input,
  InputControl,
  Select,
  TimeRangeInput,
  VerticalGroup,
} from '@grafana/ui';
import { PanelContext } from 'SimplePanel';
import _ from 'lodash';
// import { buildOptionsFromData, refresh } from './utils';
import { QueryContext } from './types';
import { buildOptionsFromData } from './utils';

const FormContext = React.createContext({});

interface FormDTO {
  invalidationId: SelectableValue<number>;
}

interface Props {
  selectedRows: MutableDataFrame;
  onClose: () => void;
  unSelect: () => void;
}

export const InvalidationLinkEditor: FC<Props> = ({ selectedRows, onClose, unSelect }) => {
  const [options, setOptions] = useState<Array<SelectableValue<number>>>([]);
  const [value, setValue] = useState<SelectableValue<number>>({});
  const [optionIsInvalid, setOptionIsInvalid] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const { dataSource, timeRange } = useContext(PanelContext);
  const queryContext = { dataSource, timeRange };
  const datatakeDbIds = selectedRows.fields.find(f => f.name === 'datatake_db_id')?.values.toArray() || [];

  // useEffect(() => {
  //   getOptions({ dataSource, timeRange }).then(options => setOptions(options));
  // }, [dataSource, timeRange]);

  const createOption = (v: string) => {
    const value = parseInt(v, 10);
    if (isNaN(value)) {
      setOptionIsInvalid(true);
      setErrorMessage(`"${v}" is not a number`);
      return;
    } else {
      setOptionIsInvalid(false);
      getInvalidationFromId(queryContext, value)
        .then(customValue => {
          setOptions([...options, customValue]);
          setValue(customValue);
        })
        .catch(reason => {
          setOptionIsInvalid(true);
          setErrorMessage(reason);
        });
    }
  };

  return (
    <Drawer
      title="Invalidation Link selection"
      subtitle="Link an existing invalidation to selected Datatakes"
      onClose={onClose}
    >
      <div>
        <Form
          onSubmit={(formData: FormDTO) =>
            onSubmit(formData, queryContext, datatakeDbIds)
              .then(unSelect)
              .then(onClose)
            // .then(refresh)
          }
        >
          {({ register, control }) => (
            <FormContext.Provider value={{ register, control }}>
              <VerticalGroup>
                <InvalidationSelect
                  options={options}
                  value={value}
                  onChange={v => {
                    setOptionIsInvalid(false);
                    setValue(v);
                  }}
                  onCreateOption={createOption}
                  optionIsInvalid={optionIsInvalid}
                  errorMessage={errorMessage}
                />
                <CollapsableSection label="Advanced Invalidation list search" isOpen={false}>
                  <InvalidationSearch onChange={setOptions} />
                </CollapsableSection>
                <HorizontalGroup>
                  <Button variant="primary">Link</Button>
                  <Button
                    variant="secondary"
                    onClick={e => {
                      e.preventDefault();
                      onClose();
                    }}
                  >
                    Cancel
                  </Button>
                </HorizontalGroup>
              </VerticalGroup>
            </FormContext.Provider>
          )}
        </Form>
      </div>
    </Drawer>
  );
};

const onSubmit = async (formData: FormDTO, queryContext: QueryContext, datatakeDbIds: number[]) => {
  const { invalidationId } = formData;
  const table = 'planned_datatake';
  const datatakeIds = datatakeDbIds.join(',');
  const rawSql = `UPDATE ${table} set invalidation_id = ${invalidationId.value} WHERE datatake_id IN(${datatakeIds})`;
  const { dataSource, timeRange } = queryContext;
  return dataSource.query(rawSql, timeRange);
};

interface InvalidationSelectProps {
  options: Array<SelectableValue<number>>;
  value: SelectableValue<number>;
  optionIsInvalid: boolean;
  errorMessage: string;
  onCreateOption: (value: string) => void;
  onChange: (v: SelectableValue<number>) => void;
}

const InvalidationSelect = ({
  options,
  value,
  onCreateOption,
  onChange,
  optionIsInvalid,
  errorMessage,
}: InvalidationSelectProps) => {
  const { control }: any = React.useContext(FormContext);
  // control.setValue('invalidationId', value);
  return (
    <Field
      label="Invalidation ID"
      invalid={optionIsInvalid}
      error={errorMessage}
    /* By default Form component assumes form elements are uncontrolled .
    There are some components like RadioButton or Select that are controlled-only and require some extra work.
    To make them work with the form, you need to render those using InputControl component */
    >
      {/* <Input
        /* Render InputControl as controlled input (Select) */
        // as={Select}
        // /* Pass control exposed from Form render prop */
        // control={control}
        // name="invalidationId"
        // defaultValue={value as any}
        // options={options}
        // // TODO : doesn't seem to work
        // rules={{ required: true }}
        // /* In case of Select the value has to be returned as an object with a `value` key for the value to be saved to form data */
        // onChange={([v]: any) => onChange(v)}
        // allowCustomValue
        // onCreateOption={onCreateOption}
        // /> */}
        <InputControl
          name="invalidationId"


          control={control}
          rules={{
            required: false,
          }}

          render={({ field }: any) => <Select {...field} onCreateOption={onCreateOption} options={options} defaultValue={value as any} onChange={([v]: any) => onChange(v)} />}


        />}
    </Field>
  );
};

interface SearchProps {
  onChange: (options: Array<SelectableValue<number>>) => void;
}

interface SearchState {
  queryContext: QueryContext;
  onChange: (options: Array<SelectableValue<number>>) => void;
}

interface InitProps {
  queryContext: QueryContext;
  onChange: (options: Array<SelectableValue<number>>) => void;
}

const init = ({ queryContext, onChange }: InitProps): SearchState => {
  return {
    queryContext,
    onChange,
  };
};

const InvalidationSearch = ({ onChange }: SearchProps) => {
  const { dataSource, timeRange } = useContext(PanelContext);
  const queryContext = { dataSource, timeRange };
  const [state, dispatch] = useReducer(reducer, { queryContext, onChange }, init);
  return (
    <Field label="Invalidation date" description="Creation or modification date of the Invalidation">
      <TimeRangeInput
        value={state.queryContext.timeRange as any}
        onChange={timeRange => dispatch({ type: 'timeRange', payload: { timeRange } })}
      />
    </Field>
  );
};

const reducer = (state: SearchState, action: { type: string; payload: any }): SearchState => {
  const { payload } = action;
  switch (action.type) {
    case 'timeRange':
      return onTimeRangeChange({ timeRange: payload.timeRange, state });
    default:
      throw new Error(`InvalidationLinkEditor Search dispatch error: unknown action '${action.type}'`);
  }
};

const onTimeRangeChange = ({ timeRange, state }: { timeRange: TimeRange; state: SearchState }) => {
  const {
    queryContext: { dataSource },
    onChange,
  } = state;
  const queryContext = { dataSource, timeRange };
  // Set options in parent component
  getOptions(queryContext).then(options => onChange(options));
  return {
    ...state,
    queryContext,
  };
};

const getOptions = async (queryContext: QueryContext) => {
  const { dataSource, timeRange } = queryContext;
  const rawSql = buildQuery({ timeRange });
  return dataSource.query(rawSql, timeRange).then((data: any) => buildOptionsFromData(data));
};

interface BuildQueryProps {
  timeRange: TimeRange;
}

const buildQuery = ({ timeRange }: BuildQueryProps) => {
  const { from: fromDate, to: toDate } = timeRange;
  const from = fromDate.toISOString();
  const to = toDate.toISOString();
  const table = 'planned_datatake';
  const query = `
      SELECT id AS value, id AS label, comment AS description FROM invalidation
      WHERE update_date  BETWEEN '${from}' AND '${to}' AND origin_table = '${table}' ORDER BY id
    `;
  return query;
};

const getInvalidationFromId = async (queryContext: QueryContext, id: number) => {
  const { dataSource, timeRange } = queryContext;
  const origin_table = 'planned_datatake';
  const rawSql = `
      SELECT id AS value, id AS label, comment AS description FROM invalidation
      WHERE id = ${id}
      AND origin_table = '${origin_table}'
      ORDER BY id
    `;
  return dataSource
    .query(rawSql, timeRange)
    .then((data: any) => buildOptionsFromData(data))
    .then((options: any) => {
      if (options.length) {
        return options[0];
      } else {
        return Promise.reject(`No invalidation found with id "${id}"`);
      }
    });
};
