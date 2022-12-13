import React, { FC, useContext, useReducer, useState, useEffect } from 'react';
import { MutableDataFrame, SelectableValue, TimeRange } from '@grafana/data';
import {
  Button,
  CollapsableSection,
  Drawer,
  Field,
  Form,
  HorizontalGroup,
  InputControl,
  Select,
  stylesFactory,
  TimeRangeInput,
  VerticalGroup,
} from '@grafana/ui';
import { PanelContext } from 'SimplePanel';
import _ from 'lodash';
import { QueryContext } from './types';
import { buildOptionsFromData, refresh } from './utils';
import { css, cx } from 'emotion';


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
  const styles = getStyles();
  const [options, setOptions] = useState<Array<SelectableValue<number>>>([]);
  const [value, setValue] = useState<SelectableValue<number>>({});
  const [optionIsInvalid, setOptionIsInvalid] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const { dataSource, timeRange } = useContext(PanelContext);
  const queryContext = { dataSource, timeRange };

  const productIds = selectedRows.fields.find(f => f.name === 'id')?.values.toArray() || [];

  useEffect(() => {

    getOptions({ dataSource, timeRange }).then(options => setOptions(options));
  }, [dataSource, timeRange]);

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

  const [isOpen, setIsOpen] = useState<boolean>(false);
  return (
    <Drawer
      title="Invalidation Link selection"
      subtitle="Link an existing invalidation to selected Products"
      onClose={onClose}
    >
      <div>
        <Form
          onSubmit={(formData: FormDTO) => {
            onSubmit(formData, queryContext, productIds)
              .then(unSelect)
              .then(onClose)
              .then(refresh)
          }
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
                <VerticalGroup>
                  <CollapsableSection className={cx(styles.collapse)} label="Advanced Invalidation list search" isOpen={isOpen} onToggle={() => setIsOpen(!isOpen)}> </CollapsableSection>
                  {isOpen ? <InvalidationSearch onChange={setOptions} /> : null}

                </VerticalGroup>
                <HorizontalGroup>
                  <Button type='submit' variant="primary">Link</Button>
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

const onSubmit = async (formData: FormDTO, queryContext: QueryContext, productIds: number[]) => {

  const { invalidationId } = formData;
  const rawSql = `UPDATE invalidation_completeness SET missing_products_ids =  array_cat(missing_products_ids,'{${productIds}}') WHERE parent_id = ${invalidationId}`
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
  return (
    <Field
      label="Invalidation ID"
      invalid={optionIsInvalid}
      error={errorMessage}

    /* By default Form component assumes form elements are uncontrolled .
    There are some components like RadioButton or Select that are controlled-only and require some extra work.
    To make them work with the form, you need to render those using InputControl component */
    >

      <InputControl
        name="invalidationId"
        defaultValue={value}
        control={control}
        rules={{
          required: false,
        }}
        render={({ field }) => <Select  {...field} width={50} allowCustomValue defaultValue={value}
          onCreateOption={onCreateOption} options={options} onChange={(e) => field.onChange(e.value)} />}
      />
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
        onChange={timeRange => {
          dispatch({ type: 'timeRange', payload: { timeRange } })
        }
        }
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

  const query = `
      SELECT id AS value, id AS label, comment AS description FROM invalidation WHERE update_date BETWEEN '${from}' AND '${to}'
    `;
  return query;
};

const getInvalidationFromId = async (queryContext: QueryContext, id: number) => {
  const { dataSource, timeRange } = queryContext;

  const rawSql = `
      SELECT id AS value, id AS label, comment AS description FROM invalidation  WHERE id = ${id} ORDER BY id 
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
const getStyles = stylesFactory(() => {
  return {
    collapse: css`
     width:'300px'
     height:'15px'
    `,

  };
});
