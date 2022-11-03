import React, { FC, useCallback, useContext, useState } from 'react';
import { dateTime, MutableDataFrame } from '@grafana/data';
import { css, cx } from 'emotion';
// Trick to make Input happy with css. See issue #26512 on Grafana's github
import { Drawer, stylesFactory, Form, FieldSet, Button, HorizontalGroup, Modal, Tooltip } from '@grafana/ui';
import { PanelContext } from 'SimplePanel';
import { refresh, unlinkInvalidations } from 'components/utils';
import { QueryContext } from 'components/types';
import { Comment, SelectCAMS, SelectCategory, SelectImpact, SelectRootCause } from './components';
import { FormDTO } from './types';
import { SELECTOPTIONS } from './options';
import { getSelectOptions } from 'components/RootCauseEditor/options';

const emptyFormData: FormDTO = {
  category: { label: 'empty', value: undefined },
  impact: { label: 'empty', value: undefined },
  rootCause: { label: 'empty', value: undefined },
  cams: { label: '-- No CAMS --', value: undefined },
  comment: '',
};

async function onSubmit(
  formData: FormDTO,
  queryContext: QueryContext,
  inval_id: number,
  datatakeDbIds: number[],
  confirmUpdateCallback: (formData: FormDTO) => void
) {
  if (inval_id == null) {
    return createInvalidation(formData, queryContext, datatakeDbIds);
  } else {
    confirmUpdateCallback(formData);
    // We reject the promise so that editor is not closed right after this function and that modal can be seen
    // TODO: better rejection for handeling it silently and not make it appear in console logs
    return Promise.reject('do not close yet');
  }
}

async function createInvalidation(formData: FormDTO, queryContext: QueryContext, datatakeDbIds: number[]) {
  const nowUtc = dateTime().utc();
  const now = nowUtc.toISOString();

  const values = {
    category: `'${formData.category.value}'`,
    impact: `'${formData.impact.value}'`,
    rootCause: formData.rootCause.value ? `'${formData.rootCause.value}'` : null,
    cams: formData.cams ? formData.cams.value : null,
    comment: formData.comment ? `'${formData.comment.replace("'", "''")}'` : null,
    update_date: `'${now}'`,
  };
  const table = `planned_datatake`;
  // Id not defined => we must create the invalidation
  const rawSql = `with created_invalidation_id as (
      insert into invalidation (
        category,
        impact,
        root_cause,
        cams_id,
        comment,
        origin_table,
        update_date)
        values (
         ${values.category},
         ${values.impact},
         ${values.rootCause},
         ${values.cams},
         ${values.comment},
         '${table}',
         ${values.update_date})
      returning id
     )
     update ${table} set invalidation_id=(select id from created_invalidation_id) where ${table}.datatake_id IN(${datatakeDbIds});
     `;
  const { dataSource, timeRange } = queryContext;
  return dataSource.query(rawSql, timeRange);
}

async function updateInvalidation(formData: FormDTO, queryContext: QueryContext, inval_id: number) {
  const nowUtc = dateTime().utc();
  const now = nowUtc.toISOString();

  const values = {
    category: `'${formData.category.value}'`,
    impact: `'${formData.impact.value}'`,
    rootCause: formData.rootCause.value ? `'${formData.rootCause.value}'` : null,
    cams: formData.cams ? formData.cams.value : null,
    comment: formData.comment ? `'${formData.comment.replace("'", "''")}'` : null,
    update_date: `'${now}'`,
  };
  const rawSql = `update invalidation
       set category = ${values.category},
       impact = ${values.impact},
       root_cause = ${values.rootCause},
       cams_id = ${values.cams},
       comment = ${values.comment},
       update_date = ${values.update_date}
       where id=${inval_id}`;

  const { dataSource, timeRange } = queryContext;
  return dataSource.query(rawSql, timeRange);
}

export const FormContext = React.createContext({});
export const SelectContext = React.createContext(SELECTOPTIONS.SENSING);

interface Props {
  selectedRows: MutableDataFrame;
  onClose: () => void;
  unSelect: () => void;
}

export const Editor: FC<Props> = ({ selectedRows, onClose, unSelect }) => {
  const styles = getStyles();
  const [confirmUpdate, setConfirmUpdate] = useState(false);
  const [confirmData, setConfirmData] = useState<FormDTO>(emptyFormData);
  const { dataSource, timeRange, options } = useContext(PanelContext);

  const queryContext = { dataSource, timeRange };
  const defaultCategory = selectedRows.fields.find(f => f.name === 'category')?.values.toArray()[0];
  const [category, setCategory] = React.useState(defaultCategory ?? 'DOWNLINK');
  const selectOption = getSelectOptions(options, category);

  // We take the first CAMS_ID available (assuming that modification will be allowed when only one DT would be selected anyway)
  const defaultCAMSId = selectedRows.fields.find(f => f.name === 'CAMS_ID')?.values.get(0);

  const datatakeDbIds = selectedRows.fields.find(f => f.name === 'datatake_db_id')?.values.toArray();
  if (!datatakeDbIds) {
    throw new Error('`datatake_db_id` column not found in data');
  }

  const defaultInval_id = selectedRows.fields.find(f => f.name === 'inval_id')?.values.toArray()[0];
  const [defaultImpact, setDefaultImpact] = useState(
    selectedRows.fields.find(f => f.name === 'impact')?.values.toArray()[0]
  ); // on edition we need to be able to set it to undefined when we change category
  const defaultComment = selectedRows.fields.find(f => f.name === 'Comment')?.values.toArray()[0];
  const initialDefaultRootCause = selectedRows.fields
    .find(f => f.name === 'INVALIDATION Root cause')
    ?.values.toArray()[0]
    ? selectedRows.fields.find(f => f.name === 'INVALIDATION Root cause')?.values.toArray()[0]
    : '';
  const [defaultRootCause, setDefaultRootCause] = useState(
    initialDefaultRootCause?.toUpperCase() === 'UNKNOWN' ? undefined : initialDefaultRootCause
  ); // on edition we need to be able to set it to null when we change category
  const changeCategory = React.useCallback(function (e: any) {
    setCategory(e.value);
    setDefaultImpact(undefined);
    setDefaultRootCause(null); // we need to set it to null as undefined is a valid value for root cause
    return e;
  }, []);

  const confirmUpdateCallback = useCallback(
    (formData: FormDTO) => {
      setConfirmUpdate(true);
      setConfirmData(formData);
    },
    [setConfirmUpdate, setConfirmData]
  );

  return (
    <div className={cx(styles.wrapper)}>
      <Drawer title="Invalidation edition" subtitle="Create or Modify an invalidation" onClose={onClose}>
        <Form
          onSubmit={(formData: FormDTO) =>
            onSubmit(formData, queryContext, defaultInval_id, datatakeDbIds, confirmUpdateCallback)
              .then(unSelect)
              .then(onClose)
              .then(refresh)
          }
        >
          {({ register, control, errors }) => (
            <>
              <FormContext.Provider value={{ register, control, errors }}>
                <SelectContext.Provider value={selectOption}>
                  <FieldSet label="Invalidation category">
                    <SelectCategory handleChange={changeCategory} valueLabel={category} />
                  </FieldSet>
                  <FieldSet label="Invalidation properties">
                    <SelectImpact defaultValue={defaultImpact} />
                    <SelectRootCause defaultValue={defaultRootCause} />
                    <SelectCAMS defaultCAMSId={defaultCAMSId} />
                    <Comment defaultValue={defaultComment} />
                  </FieldSet>
                  <HorizontalGroup>
                    <Button variant="primary">Save</Button>
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
                </SelectContext.Provider>
              </FormContext.Provider>
            </>
          )}
        </Form>
        <Modal isOpen={confirmUpdate} title="Choose update impact" onDismiss={() => setConfirmUpdate(false)}>
          <p className={cx(styles.modalBody)}>
            Should this update impact ONE datatake or ALL datatakes linked to this invalidation?
          </p>
          <HorizontalGroup justify="center">
            <Tooltip content="Will affect only the SELECTED datatake">
              <Button
                onClick={() => {
                  unlinkInvalidations(queryContext, datatakeDbIds)
                    .then(r => createInvalidation(confirmData, queryContext, datatakeDbIds))
                    .then(r => setConfirmUpdate(false))
                    .then(r => unSelect())
                    .then(r => onClose())
                    .then(refresh);
                }}
              >
                Create new invalidation
              </Button>
            </Tooltip>
            <Tooltip content="Will affect ALL datatakes linked to this invalidation">
              <Button
                variant="destructive"
                onClick={() => {
                  updateInvalidation(confirmData, queryContext, defaultInval_id)
                    .then(r => setConfirmUpdate(false))
                    .then(r => unSelect())
                    .then(r => onClose())
                    .then(refresh);
                }}
              >
                Update invalidation
              </Button>
            </Tooltip>
          </HorizontalGroup>
        </Modal>
      </Drawer>
    </div>
  );
};

const getStyles = stylesFactory(() => {
  return {
    wrapper: css`
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: auto;
    `,
    modalBody: css`
      text-align: center;
    `,
  };
});
