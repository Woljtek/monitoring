import React, { FC, useCallback, useContext, useState } from 'react';
import { dateTime, MutableDataFrame, SelectableValue } from '@grafana/data';
import { css, cx } from 'emotion';
// Trick to make Input happy with css. See issue #26512 on Grafana's github
import { Drawer, stylesFactory, Form, FieldSet, Input, HorizontalGroup, Button, Field, TextArea, InputControl, Select, Modal, Tooltip } from '@grafana/ui';
import { PanelContext } from 'SimplePanel';

import { QueryContext } from 'components/types';
import { EResponsibility, FormDTO } from './types';
import { CATEGORYOPTIONS, SELECTOPTIONS } from './options';
// import { SelectCategory } from './components/SelectCategory';
import { refresh, unlinkInvalidations } from 'components/utils';
// import { getSelectOptions } from 'components/RootCauseEditor/options';

// import { getSelectOptions } from 'components/RootCauseEditor/options';


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
    responsibility: formData.responsibility ? `'${formData.responsibility.value}'` : null,
    rootCause: formData.rootCause ? `'${formData.rootCause.value}'` : null,
    comment: formData.comment ? `'${formData.comment.replace("'", "''")}'` : null,
    label: formData.label ? `'${formData.label.replace("'", "''")}'` : null,
    anomalyIdentifier: formData.anomalyIdentifier !== 0 ? formData.anomalyIdentifier : null,
    update_date: `'${now}'`,
  };
  const table = `invalidation_timeliness`;
  // if (datatakeDbIds.length === 1) {

  //   console.log("formData", formData)

  //   console.log("selectedrow", datatakeDbIds[0])

  //   // Id not defined => we must create the invalidation
  //   const rawSql =
  //     // `with created_id as (
  //     //     insert into invalidation (
  //     //       responsibility,
  //     //       label,
  //     //       root_cause,
  //     //       anomaly_identifier,
  //     //       comment,
  //     //       update_date)
  //     //       values (
  //     //        ${values.responsibility},
  //     //        ${values.label},
  //     //        ${values.rootCause}, 
  //     //        ${values.anomalyIdentifier},
  //     //        ${values.comment},
  //     //        ${values.update_date})
  //     //     returning id
  //     //    )
  //     //    update ${table} set parent_id=(select id from created_id),
  //     //    product_id = (${datatakeDbIds[0]})
  //     //    `;
  //     `with created_id as (
  //     insert into invalidation (
  //      responsibility,
  //      label,
  //      root_cause,
  //      anomaly_identifier,
  //      comment,
  //      update_date)
  //      values (
  //       ${values.responsibility},
  //       ${values.label},
  //       ${values.rootCause}, 
  //       ${values.anomalyIdentifier},
  //       ${values.comment},
  //       ${values.update_date})
  //       returning id
  //   )
  //   insert into ${table} (
  //     parent_id,
  //   product_id)
  //   values (
  //   (select id from created_id),
  //    ${datatakeDbIds[0]}
  //   )
  //   `;
  //   const { dataSource, timeRange } = queryContext;
  //   return dataSource.query(rawSql, timeRange);
  // }
  // else {
  // datatakeDbIds.map(async (datatakeDbId: number) => {

  // let sql = ``;
  // datatakeDbIds.map((datatakeDbId: number, index: number) => {
  //   sql = sql +
  //     `((select id from created_id),
  //     ${datatakeDbId}),
  //     `;
  //   return datatakeDbId
  // })

  const rawSql =
    // `with created_id as (
    //     insert into invalidation (
    //       responsibility,
    //       label,
    //       root_cause,
    //       anomaly_identifier,
    //       comment,
    //       update_date)
    //       values (
    //        ${values.responsibility},
    //        ${values.label},
    //        ${values.rootCause}, 
    //        ${values.anomalyIdentifier},
    //        ${values.comment},
    //        ${values.update_date})
    //     returning id
    //    )
    //    update ${table} set parent_id=(select id from created_id),
    //    product_id = (${datatakeDbIds[0]})
    //    `;
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
           '{${datatakeDbIds}}')
        `;
  // update ${table} set invalidation_id=(select id from created_invalidation_id) where ${table}.id IN(${sliceDbIds});

  //   const essai = `with created_id as (
  // insert into invalidation (
  //    responsibility,
  //    label,
  //    root_cause,
  //    anomaly_identifier,
  //    comment,
  //    update_date)
  //    values (
  //     'E2E',
  //     'dsfsfd',
  //     'MTI', 
  //     8899,
  //     'sdfdsf',
  //     '2022-11-14T12:51:03.216Z')
  //     returning id
  // )
  // insert into invalidation_timeliness (
  //   parent_id,
  //   product_ids
  // )
  // values 
  // ((select id from created_id),$()),
  // ((select id from created_id),2334);

  console.log("requete", rawSql, datatakeDbIds)
  const { dataSource, timeRange } = queryContext;
  return dataSource.query(rawSql, timeRange);

  // })
}
// }

async function updateInvalidation(formData: FormDTO, queryContext: QueryContext, inval_id: number) {
  const nowUtc = dateTime().utc();
  const now = nowUtc.toISOString();
  console.log("anomaliid", formData.anomalyIdentifier, typeof formData.anomalyIdentifier)
  const values = {
    responsibility: formData.responsibility ? `'${formData.responsibility.value}'` : null,
    rootCause: formData.rootCause ? `'${formData.rootCause.value}'` : null,
    comment: formData.comment ? `'${formData.comment.replace("'", "''")}'` : null,
    label: formData.label ? `'${formData.label.replace("'", "''")}'` : null,
    anomalyIdentifier: formData.anomalyIdentifier > 0 ? formData.anomalyIdentifier : null,
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
  console.log("rawSqlUpdate", rawSql)
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
  const defaultResponsibility = selectedRows.fields.find(f => f.name === 'responsibility')?.values.toArray()[0];
  const defaultLabel = selectedRows.fields.find(f => f.name === 'label')?.values.toArray()[0];
  const defaultAnomalyIdentifier = selectedRows.fields.find(f => f.name === 'anomaly_identifier')?.values.toArray()[0];
  const [responsibility,] = React.useState<SelectableValue<EResponsibility>>(defaultResponsibility ?? emptyFormData.responsibility);
  const initialDefaultRootCause = selectedRows.fields.find(f => f.name === 'root_cause')?.values.toArray()[0];
  const [defaultRootCause] = React.useState<SelectableValue<string>>(initialDefaultRootCause ?? SELECTOPTIONS.TIMELINESS.rootCause.options[0]);
  const [anomalyIdentifier] = React.useState<number>(defaultAnomalyIdentifier);
  const [label] = React.useState(defaultLabel ?? "");


  // We take the first CAMS_ID available (assuming that modification will be allowed when only one DT would be selected anyway)
  // const defaultCAMSId = selectedRows.fields.find(f => f.name === 'CAMS_ID')?.values.get(0);

  const datatakeDbIds = selectedRows.fields.find(f => f.name === 'id')?.values.toArray();
  if (!datatakeDbIds) {
    throw new Error('`id` column not found in data');
  }
  const selectOption = options.rootCauseListTimeliness;
  const defaultInval_id = selectedRows.fields.find(f => f.name === 'inval_id')?.values.toArray()[0];
  // const [, setDefaultImpact] = useState(
  //   selectedRows.fields.find(f => f.name === 'impact')?.values.toArray()[0]
  // ); // on edition we need to be able to set it to undefined when we change category
  const defaultComment = selectedRows.fields.find(f => f.name === 'comment')?.values.toArray()[0];
  const [comment] = React.useState(defaultComment ?? "");
  const { rootCause } = React.useContext(SelectContext);

  // const initialDefaultRootCause = selectedRows.fields
  //   .find(f => f.name === 'root_cause')
  //   ?.values.toArray()[0]
  //   ? selectedRows.fields.find(f => f.name === 'root_cause')?.values.toArray()[0]
  //   : '';
  // const [defaultRootCause, setDefaultRootCause] = useState<SelectableValue<string>>(
  //   initialDefaultRootCause?.toUpperCase() === 'UNKNOWN' ? undefined : initialDefaultRootCause
  // ); // on edition we need to be able to set it to null when we change category
  // const changeResponsibility = React.useCallback(function (e: any) {
  //   setResponsibility(e.value);
  //   // setDefaultImpact(undefined);
  //   // setDefaultRootCause(null); // we need to set it to null as undefined is a valid value for root cause
  //   return e;
  // }, []);

  // const { rootCause } = React.useContext(SelectContext);
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
          onSubmit={(formData: FormDTO) => {
            onSubmit(formData, queryContext, defaultInval_id, datatakeDbIds, confirmUpdateCallback)
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
                      // defaultValue={value}
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
                    //  onChange={setLabel}
                    defaultValue={label}
                    {...register('label', { required: true })}
                  />
                  {/* <Input required defaultValue={label} onChange={setLabel} ref={register as any} /> */}


                </FieldSet>

                <FieldSet label="Invalidation properties">
                  {/* <SelectImpact defaultValue={defaultImpact} /> */}
                  {/* <SelectRootCause defaultValue={defaultRootCause} handleChange={setDefaultRootCause} /> */}
                  <Field label="Anomaly identifier">
                    <Input
                      required
                      type='number'
                      //  onChange={setLabel}
                      defaultValue={anomalyIdentifier}
                      {...register('anomalyIdentifier', {
                        valueAsNumber: true,
                      })}
                    />
                    {/* <Input required defaultValue={label} onChange={setLabel} ref={register as any} /> */}


                  </Field>
                  <Field label="Root Cause" invalid={!!errors.rootCause} error="Root cause is required">

                    <InputControl
                      name="rootCause"

                      control={control}
                      rules={{ required: true }}
                      defaultValue={defaultRootCause}
                      // defaultValue={value}
                      render={({ field: { onChange, onBlur, value, name, ref } }) => (
                        <Select
                          options={selectOption}
                          value={value}
                          onChange={onChange}
                          disabled={rootCause.disabled}
                        />)}



                    />
                  </Field>
                  {/*<SelectCAMS defaultCAMSId={defaultCAMSId} /> */}
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
