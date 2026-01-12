
export const formatRatingFrom10to5 = (rating: number | null): number => {
  if (rating === null) {
    return 0;
  }
  const converted = rating / 2;
  return Math.round(converted * 10) / 10;
};


export const formatRatingForDisplay = (rating: number): string => {
  return rating.toFixed(1);
};