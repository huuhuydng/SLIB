const MIN_SAFE_TEMP_INT_ID = -2147483648;

const normalizeId = (value) => {
  if (typeof value === "number" && Number.isInteger(value)) {
    return value;
  }

  if (typeof value === "string" && value.trim() !== "") {
    const parsed = Number(value);
    if (Number.isInteger(parsed)) {
      return parsed;
    }
  }

  return null;
};

export const getNextTempIntId = (...sources) => {
  const allIds = sources.flatMap((source) => {
    if (!source) return [];
    if (Array.isArray(source)) return source;
    return [source];
  });

  const minExistingNegativeId = allIds.reduce((minId, value) => {
    const normalizedId = normalizeId(value);
    if (normalizedId === null || normalizedId >= 0) {
      return minId;
    }
    return Math.min(minId, normalizedId);
  }, 0);

  const nextId = minExistingNegativeId - 1;
  return nextId <= MIN_SAFE_TEMP_INT_ID ? -1 : nextId;
};
