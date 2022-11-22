import React, { FC, useCallback, useContext, useState } from 'react';
import { dateTime, MutableDataFrame, SelectableValue } from '@grafana/data';
import { css, cx } from 'emotion';
// Trick to make Input happy with css. See issue #26512 on Grafana's github
import { Drawer, stylesFactory, Form, FieldSet, Input, HorizontalGroup, Button, Field, TextArea, InputControl, Select, Modal, Tooltip } from '@grafana/ui';
import { PanelContext } from 'SimplePanel';
import { QueryContext } from 'components/types';
import { EResponsibility, FormDTO } from './types';
import { CATEGORYOPTIONS, SELECTOPTIONS } from './options';
import { refresh, unlinkInvalidations } from 'components/utils';

const emptyFormData: FormDTO = {
  responsibility: CATEGORYOPTIONS[0],
  label: '',
  rootCause: { label: 'empty', value: undefined },
  anomalyIdentifier: 0,
  comment: '',
};

async function onSubmit(
  formData: FormDTO,
  queryContext: QueryContext,
  inval_id: number,
  productIds: number[],
  confirmUpdateCallback: (formData: FormDTO) => void
) {
  if (inval_id == null) {
    return createInvalidation(formData, queryContext, productIds);
  } else {
    confirmUpdateCallback(formData);
    // We reject the promise so that editor is not closed right after this function and that modal can be seen
    // TODO: better rejection for handeling it silently and not make it appear in console logs
    return Promise.reject('do not close yet');
  }
}

async function createInvalidation(formData: FormDTO, queryContext: QueryContext, productIds: number[]) {
  const nowUtc = dateTime().utc();
  const now = nowUtc.toISOString();
  const values = {
    responsibility: formData.responsibility ? `'${formData.responsibility.value}'` : null,
    rootCause: formData.rootCause ? `'${formData.rootCause.value}'` : null,
    comment: formData.comment ? `'${formData.comment.replace("'", "''")}'` : null,
    label: formData.label ? `'${formData.label.replace("'", "''")}'` : null,
    anomalyIdentifier: formData.anomalyIdentifier !== 0 ? formData.anomalyIdentifier : null,
    update_date: `'${now}'`,
  };
  const table = `invalidation_timeliness`;
  const rawSql =
    `with created_id as (
          insert into invalidation (
           responsibility,
           label,
           root_cause,
           anomaly_identifier,
           comment,
           update_date)
           values (
            ${values.responsibility},
            ${values.label},
            ${values.rootCause}, 
            ${values.anomalyIdentifier},
            ${values.comment},
            ${values.update_date})
            returning id
        )
        insert into ${table} (
          parent_id,
          product_ids)
          values (
          (select id from created_id),
           '{${productIds}}')
        `;
  const { dataSource, timeRange } = queryContext;
  return dataSource.query(rawSql, timeRange);
}

async function updateInvalidation(formData: FormDTO, queryContext: QueryContext, inval_id: number) {
  const nowUtc = dateTime().utc();
  const now = nowUtc.toISOString();
  const values = {
    responsibility: formData.responsibility ? `'${formData.responsibility.value}'` : null,
    rootCause: formData.rootCause ? `'${formData.rootCause.value}'` : null,
    comment: formData.comment ? `'${formData.comment.replace("'", "''")}'` : null,
    label: formData.label ? `'${formData.label.replace("'", "''")}'` : null,
    anomalyIdentifier: formData.anomalyIdentifier > 0 ? `${formData.anomalyIdentifier}` : null,
    update_date: `'${now}'`,
  };

  const rawSql = `update invalidation
       set responsibility = ${values.responsibility},
       root_cause = ${values.rootCause},
       comment = ${values.comment},
       label = ${values.label},
       anomaly_identifier = ${values.anomalyIdentifier},
       update_date = ${values.update_date}
       where id=${inval_id}`;

  const { dataSource, timeRange } = queryContext;
  return dataSource.query(rawSql, timeRange);
}

export const FormContext = React.createContext({});
export const SelectContext = React.createContext(SELECTOPTIONS.TIMELINESS);

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
  const defaultComment = selectedRows.fields.find(f => f.name === 'comment')?.values.toArray()[0];
  const defaultResponsibility = selectedRows.fields.find(f => f.name === 'responsibility')?.values.toArray()[0];
  const defaultLabel = selectedRows.fields.find(f => f.name === 'label')?.values.toArray()[0];
  const defaultAnomalyIdentifier = selectedRows.fields.find(f => f.name === 'anomaly_identifier')?.values.toArray()[0];
  const [responsibility,] = React.useState<SelectableValue<EResponsibility>>(defaultResponsibility ? CATEGORYOPTIONS.filter((f) => f.value === defaultResponsibility)[0] : emptyFormData.responsibility);
  const initialDefaultRootCause: string = selectedRows.fields.find(f => f.name === 'root_cause')?.values.toArray()[0];
  const [defaultRootCause] = React.useState<SelectableValue<string>>(initialDefaultRootCause ? options.rootCauseListTimeliness.filter((f) => f.value === initialDefaultRootCause)[0] : SELECTOPTIONS.TIMELINESS.rootCause.options[0]);
  console.log('ini', initialDefaultRootCause, defaultRootCause, defaultResponsibility, responsibility)
  const [anomalyIdentifier] = React.useState<number>(defaultAnomalyIdentifier ?? undefined);
  const [label] = React.useState(defaultLabel ?? "");
  const productIds = selectedRows.fields.find(f => f.name === 'id')?.values.toArray();
  const selectOption = options.rootCauseListTimeliness;
  const defaultInval_id = selectedRows.fields.find(f => f.name === 'inval_id')?.values.toArray()[0];
  const [comment] = React.useState(defaultComment ?? "");
  const { rootCause } = React.useContext(SelectContext);
  if (!productIds) {
    throw new Error('`id` column not found in data');
  }

  const confirmUpdateCallback = useCallback(
    (formData: FormDTO) => {
      setConfirmUpdate(true);
      setConfirmData(formData);
    },
    [setConfirmUpdate, setConfirmData]
  );
  console.log("selectedrow", selectedRows.fields)
  return (
    <div className={cx(styles.wrapper)}>
      <Drawer title="Invalidation edition" subtitle="Create or Modify an invalidation" onClose={onClose}>
        <Form
          onSubmit={(formData: FormDTO) => {
            onSubmit(formData, queryContext, defaultInval_id, productIds, confirmUpdateCallback)
              .then(unSelect)
              .then(onClose)
              .then(refresh)
          }}
        >
          {({ register, control, errors }) => (
            <>
              <FormContext.Provider value={{ register, control, errors }}>

                <FieldSet label="Invalidation responsibility">
                  <Field label="Responsibility" invalid={!!errors.responsibility} error="Responsibility is required">

                    <InputControl
                      name="responsibility"
                      control={control}
                      rules={{
                        required: true,
                      }}
                      defaultValue={responsibility}
                      render={({ field: { onChange, onBlur, value, name, ref } }) => (
                        <Select
                          options={CATEGORYOPTIONS}
                          value={value}
                          onChange={onChange}
                        />)}
                    />

                  </Field>
                </FieldSet>
                <FieldSet label="Invalidation label">
                  <Input
                    required
                    defaultValue={label}
                    {...register('label', { required: true })}
                  />
                </FieldSet>
                <FieldSet label="Invalidation properties">
                  <Field label="Anomaly identifier">
                    <Input
                      required
                      type='number'
                      defaultValue={String(anomalyIdentifier)}
                      {...register('anomalyIdentifier', {
                        valueAsNumber: true,
                      })}
                    />
                  </Field>
                  <Field label="Root Cause" invalid={!!errors.rootCause} error="Root cause is required">
                    <InputControl
                      name="rootCause"
                      control={control}
                      rules={{ required: true }}
                      defaultValue={defaultRootCause}
                      render={({ field: { onChange, value, ref } }) => (
                        <Select
                          options={selectOption}
                          value={value}
                          onChange={onChange}
                          disabled={rootCause.disabled}
                        />)}
                    />
                  </Field>
                  <Field label="Comment">
                    <TextArea {...register('comment')} defaultValue={comment} placeholder="Free comment" contentEditable draggable />
                  </Field>
                </FieldSet>
                <HorizontalGroup>
                  <Button type='submit' variant="primary">Save</Button>
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

              </FormContext.Provider>
            </>
          )}
        </Form>
        <Modal isOpen={confirmUpdate} title="Choose update impact" onDismiss={() => setConfirmUpdate(false)}>
          <p className={cx(styles.modalBody)}>
            Should this update impact ONE product or ALL products linked to this invalidation?
          </p>
          <HorizontalGroup justify="center">
            <Tooltip content="Will affect only the SELECTED product">
              <Button
                onClick={() => {
                  unlinkInvalidations(queryContext, productIds)
                    .then(r => createInvalidation(confirmData, queryContext, productIds))
                    .then(r => setConfirmUpdate(false))
                    .then(r => unSelect())
                    .then(r => onClose())
                    .then(refresh);
                }}
              >
                Create new invalidation
              </Button>
            </Tooltip>
            <Tooltip content="Will affect ALL products linked to this invalidation">
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
    </div >
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
