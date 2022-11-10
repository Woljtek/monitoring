// import React, { useContext, useEffect, useReducer, useState } from 'react';
// import { css, cx } from 'emotion';
// import {
//   CollapsableSection,
//   Field,
//   TimeRangeInput,
//   HorizontalGroup,
//   stylesFactory,
//   Input,
// } from '@grafana/ui';
// import { FormContext } from '../Editor';
// import { SelectableValue, TimeRange } from '@grafana/data';
// import { PanelContext } from 'SimplePanel';
// import _ from 'lodash';
// import { buildOptionsFromData, DataSource } from 'components/utils';

// interface SelectCAMSParameters {
//   defaultCAMSId: number;
// }

// export const SelectCAMS = ({ defaultCAMSId }: SelectCAMSParameters) => {
//   const styles = getStyles();
//   const { errors, control }: any = React.useContext(FormContext);
//   const { dataSource, timeRange } = useContext(PanelContext);
//   // We can't call async method to retreive options in useState so we need a default option array
//   const initalOptions = [{ label: '-- No CAMS --', value: undefined }];
//   const [options, setOptions] = useState<Array<SelectableValue<number | undefined>>>(initalOptions);

//   // Set true initial options
//   useEffect(() => {
//     getOptions({ dataSource, timeRange, defaultCAMSId }).then(options => setOptions(options));
//   }, [dataSource, timeRange, defaultCAMSId]);

//   const optionValue = options?.find(o => o.value === defaultCAMSId);
//   control.setValue('cams', optionValue);

//   return (
//     <HorizontalGroup justify="space-between">
//       <div className={cx(styles.cams)} /*FIXME: style doesn't seem to work */>
//         <Field
//           label="CAMS"
//           invalid={!!errors.cams}
//           error="Root cause is required"
//         /* By default Form component assumes form elements are uncontrolled .
//     There are some components like RadioButton or Select that are controlled-only and require some extra work.
//     To make them work with the form, you need to render those using InputControl component */
//         >
//           <Input
//             /* Render InputControl as controlled input (Select) */

//             defaultValue={optionValue as any}
//           />
//         </Field>
//       </div>
//       <CollapsableSection label="Advanced CAMS list search" isOpen={false}>
//         <CAMSQueryEditor defaultCAMSId={defaultCAMSId} onChange={setOptions} />
//       </CollapsableSection>
//     </HorizontalGroup>
//   );
// };

// interface CAMSQueryEditorState {
//   defaultCAMSId: number | undefined;
//   dataSource: DataSource;
//   timeRange: TimeRange;
//   onChange: (options: Array<SelectableValue<number | undefined>>) => void;
// }

// const init = ({
//   dataSource,
//   defaultCAMSId,
//   timeRange,
//   onChange,
// }: {
//   dataSource: DataSource;
//   defaultCAMSId: number | undefined;
//   timeRange: TimeRange;
//   onChange: (options: Array<SelectableValue<number | undefined>>) => void;
// }): CAMSQueryEditorState => ({
//   dataSource,
//   timeRange,
//   defaultCAMSId,
//   onChange: onChange,
// });

// const reducer = (state: CAMSQueryEditorState, action: { type: string; payload: any }): CAMSQueryEditorState => {
//   const { payload } = action;
//   switch (action.type) {
//     case 'timeRange':
//       return onTimeRangeChange({ timeRange: payload.timeRange, state });
//     default:
//       throw new Error(`CAMSQueryEditor dispatch error: unknown action '${action.type}'`);
//   }
// };

// const onTimeRangeChange = ({ timeRange, state }: { timeRange: TimeRange; state: CAMSQueryEditorState }) => {
//   const { defaultCAMSId, dataSource, onChange } = state;
//   // Set options in parent component
//   getOptions({ dataSource, defaultCAMSId, timeRange }).then(options => onChange(options));
//   return {
//     ...state,
//     timeRange,
//   };
// };

// const getOptions = async ({
//   dataSource,
//   timeRange,
//   defaultCAMSId,
// }: {
//   dataSource: DataSource;
//   timeRange: TimeRange;
//   defaultCAMSId: number | undefined;
// }) => {
//   const rawSql = buildQuery({ defaultCAMSId, timeRange });
//   return dataSource.query(rawSql, timeRange).then(data => buildOptionsFromData(data));
// };

// interface CAMSQueryEditorProps {
//   defaultCAMSId: number | undefined;
//   onChange: (options: Array<SelectableValue<number | undefined>>) => void;
// }

// const CAMSQueryEditor = ({ defaultCAMSId, onChange }: CAMSQueryEditorProps) => {
//   const { dataSource, timeRange } = useContext(PanelContext);
//   const [state, dispatch] = useReducer(reducer, { dataSource, defaultCAMSId, timeRange, onChange }, init);
//   return (
//     <>
//       <Field label="CAMS date" description="Creation or modification date of the CAMS">
//         <TimeRangeInput
//           value={state.timeRange as any}
//           onChange={timeRange => dispatch({ type: 'timeRange', payload: { timeRange } })}
//         />
//       </Field>
//     </>
//   );
// };

// interface BuildQueryProps {
//   defaultCAMSId: number | undefined;
//   timeRange: TimeRange;
// }

// const buildQuery = ({ defaultCAMSId, timeRange }: BuildQueryProps) => {
//   const { from: fromDate, to: toDate } = timeRange;
//   const from = fromDate.toISOString();
//   const to = toDate.toISOString();
//   const idCondition =
//     defaultCAMSId == null // check if null or undefined
//       ? ''
//       : `UNION
//       SELECT id AS value, pdgsanom_id AS label, description AS description FROM cams WHERE id=${defaultCAMSId}`;
//   const query = `
//       SELECT id AS value, pdgsanom_id AS label, description AS description FROM cams
//       WHERE update_date  BETWEEN '${from}' AND '${to}' OR creation_date BETWEEN '${from}' AND '${to}'
//       UNION
//       SELECT null AS value, '-- No CAMS --' AS label, 'No CAMS is associated to this product' AS description
//       ${idCondition}
//       ORDER BY 1 NULLS FIRST
//     `;
//   return query;
// };

// const getStyles = stylesFactory(() => {
//   return {
//     cams: css`
//       width: 100%;
//     `,
//   };
// });
