// Code adapted from grafana-ui/src/components/Table/JSONViewCell.tsx
import React, { FC } from 'react';
import { css, cx } from 'emotion';
import { isString } from 'lodash';
import { GrafanaTheme } from '@grafana/data';
import { Tooltip, useStyles, JSONFormatter } from '@grafana/ui';

interface JSONViewCellProps {
  cellValue: any;
  tooltipValue?: any;
}

/*
Display `cellValue` with ellipsis on cell size and a tooltip formatted in JSON.
If `tooltipValue` is provided it will be used in place of `cellValue` content (still formatted in JSON).
*/
export const JSONViewCell: FC<JSONViewCellProps> = ({ cellValue, tooltipValue }) => {
  const txt = css`
    cursor: pointer;
    font-family: monospace;
  `;

  const padding = 2;
  // grafana-ui/src/components/Table/styles.ts
  const tableStyles = {
    tableCell: css`
      padding: ${padding}px;
      text-overflow: ellipsis;
      white-space: nowrap;
      overflow: hidden;
    `,
    overflow: css`
      overflow: hidden;
      text-overflow: ellipsis;
    `,
  };

  let displayValue = cellValue;
  if (isString(cellValue)) {
    try {
      cellValue = JSON.parse(cellValue);
    } catch { } // ignore errors
  } else {
    displayValue = JSON.stringify(cellValue);
  }
  if (tooltipValue && isString(tooltipValue)) {
    try {
      tooltipValue = JSON.parse(tooltipValue);
    } catch { }
  } else {
    tooltipValue = cellValue;
  }
  const content = <JSONTooltip value={tooltipValue} />;
  return (
    <div className={cx(txt, tableStyles.tableCell)}>
      <Tooltip placement="auto" content={content} theme="info-alt">
        <div className={tableStyles.overflow}>{displayValue}</div>
      </Tooltip>
    </div>
  );
};

interface PopupProps {
  value: any;
}

const JSONTooltip: FC<PopupProps> = props => {
  const styles = useStyles((theme: GrafanaTheme) => {
    return {
      container: css`
        padding: ${theme.spacing.xs};
      `,
    };
  });

  return (
    <div className={styles.container}>
      <div>
        <JSONFormatter json={props.value} open={4} />
      </div>
    </div>
  );
};
