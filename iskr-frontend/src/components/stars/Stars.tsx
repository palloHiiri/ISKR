import Star from "../../assets/elements/star.svg";
import SemiStar from "../../assets/elements/semi-star.svg";
import NullStar from "../../assets/elements/null-star.svg";
import './Stars.scss';

interface StarsProps {
  count: number;
  onChange?: (count: number) => void;
  size?: 'small' | 'medium' | 'large';
  showValue?: boolean;
}

function Stars({ count, onChange, size = 'medium', showValue = true }: StarsProps) {
  const roundedCount = Math.round(count * 2) / 2;
  
  const handleStarClick = (starValue: number) => {
    if (onChange) {
      if (starValue === Math.ceil(count) && count === starValue) {
        onChange(starValue - 0.5);
      } else {
        onChange(starValue);
      }
    }
  };

  const handleHalfStarClick = (starValue: number, e: React.MouseEvent) => {
    if (onChange) {
      const starElement = e.currentTarget;
      const rect = starElement.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const width = rect.width;
      
      if (x > width / 2) {
        onChange(starValue);
      } else {
        onChange(starValue - 0.5);
      }
    }
  };

  const renderStar = (position: number) => {
    const isClickable = !!onChange;
    
    let starElement;
    if (roundedCount >= position) {
      starElement = (
        <div 
          className="star-full"
          onClick={isClickable ? (e) => handleHalfStarClick(position, e) : undefined}
          style={{ cursor: isClickable ? 'pointer' : 'default' }}
        >
          <img src={Star} alt="star" />
        </div>
      );
    } else if (roundedCount >= position - 0.5) {
      starElement = (
        <div className="star-half-container">
          <img src={NullStar} alt="empty-star" />
          <div 
            className="star-half"
            onClick={isClickable ? (e) => handleHalfStarClick(position, e) : undefined}
            style={{ cursor: isClickable ? 'pointer' : 'default' }}
          >
            <img src={SemiStar} alt="half-star" />
          </div>
        </div>
      );
    } else {
      starElement = (
        <div 
          className="star-empty"
          onClick={isClickable ? (e) => handleHalfStarClick(position, e) : undefined}
          style={{ cursor: isClickable ? 'pointer' : 'default' }}
        >
          <img src={NullStar} alt="empty-star" />
        </div>
      );
    }

    return (
      <div
        className={`star ${size}`}
        key={position}
      >
        {starElement}
      </div>
    );
  };

  return (
    <div className="stars-container">
      {[1, 2, 3, 4, 5].map(renderStar)}
      {showValue && (
        <span className="rating-value">
          {count.toFixed(1)}
        </span>
      )}
    </div>
  );
}

export default Stars;