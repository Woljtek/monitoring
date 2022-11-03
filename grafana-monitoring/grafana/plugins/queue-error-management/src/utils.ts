export const countArrayElements = (array: any[]): { [key: number]: number } | { [key: string]: number } =>
  array.reduce((allElements, element) => {
    if (element in allElements) {
      allElements[element]++;
    } else {
      allElements[element] = 1;
    }
    return allElements;
  }, {});
