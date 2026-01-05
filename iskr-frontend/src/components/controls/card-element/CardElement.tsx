import './CardElement.scss';
import Stars from "../../stars/Stars.tsx";
import {useState, useEffect} from "react";

interface CardElementProps {
  title: string;
  description: string;
  imageUrl: string;
  onClick?: () => void;
  button: boolean;
  buttonLabel?: string;
  buttonIconUrl?: string;
  onButtonClick?: () => void;
  starsCount?: number;
  infoDecoration?: string;
  buttonChanged?: boolean;
  buttonChangedLabel?: string;
  buttonChangedIconUrl?: string;
  isButtonActive?: boolean;
  isAuthenticated?: boolean;
  onUnauthorized?: () => void;
  buttonClicked?: boolean;
  starsSize?: 'small' | 'medium' | 'large';
  isInCollection?: boolean;
  removeButtonLabel?: string;
  addButtonLabel?: string;
}

function CardElement({
  title,
  description,
  imageUrl,
  onClick,
  button,
  buttonLabel,
  buttonIconUrl,
  onButtonClick,
  starsCount,
  infoDecoration,
  buttonChanged,
  buttonChangedLabel,
  buttonChangedIconUrl,
  isButtonActive = false,
  isAuthenticated = true,
  onUnauthorized,
  buttonClicked = false,
  starsSize = 'small',
  isInCollection = false,
  removeButtonLabel = "Убрать из коллекции",
  addButtonLabel = "Добавить в коллекцию"
}: CardElementProps) {
  const [clicked, setClicked] = useState(buttonClicked);
  const [buttonImg, setButtonImg] = useState(buttonIconUrl);
  const [buttonLbl, setButtonLbl] = useState(buttonLabel || (isInCollection ? removeButtonLabel : addButtonLabel));

  useEffect(() => {
    if (!buttonChanged) {
      const newLabel = isInCollection ? removeButtonLabel : addButtonLabel;
      setButtonLbl(newLabel);
      setClicked(isInCollection);
    }
  }, [isInCollection, buttonChanged, removeButtonLabel, addButtonLabel]);

  useEffect(() => {
    setClicked(buttonClicked);
  }, [buttonClicked]);

  useEffect(() => {
    if (buttonLabel) {
      setButtonLbl(buttonLabel);
    }
  }, [buttonLabel]);

  useEffect(() => {
    if (buttonChanged) {
      if (buttonIconUrl && buttonChangedIconUrl) {
        setButtonImg(isButtonActive ? buttonChangedIconUrl : buttonIconUrl);
      }
      if (buttonLabel && buttonChangedLabel) {
        setButtonLbl(isButtonActive ? buttonChangedLabel : buttonLabel);
      }
      setClicked(isButtonActive);
    }
  }, [isButtonActive, buttonChanged, buttonIconUrl, buttonChangedIconUrl, buttonLabel, buttonChangedLabel]);

  const handleClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    
    if (!isAuthenticated) {
      onUnauthorized?.();
      return;
    }

    if (buttonChanged) {
      setClicked(!clicked);
      if (buttonIconUrl && buttonChangedIconUrl) {
        setButtonImg(buttonImg === buttonIconUrl ? buttonChangedIconUrl : buttonIconUrl);
      }
      if (buttonLabel && buttonChangedLabel) {
        setButtonLbl(buttonLbl === buttonLabel ? buttonChangedLabel : buttonLabel);
      }
    } else {
      setClicked(!clicked);
      const newLabel = clicked ? addButtonLabel : removeButtonLabel;
      setButtonLbl(newLabel);
    }
    onButtonClick?.();
  }

  return (
    <div className="card-element" onClick={onClick} style={onClick ? { cursor: 'pointer' } : undefined}>
      <div className="top-card-container">
        <img src={imageUrl} alt={title} className="card-image" />
      </div>
      <div className="card-info">
        {(starsCount || infoDecoration) &&
          <div className="card-info-decoration">
            {starsCount ? <Stars count={starsCount} size={starsSize} showValue={true}/> : null}
            {infoDecoration ? (
              <span className={`info-decoration-text ${isInCollection ? 'in-collection' : ''}`}>
                {infoDecoration}
              </span>
            ) : null}
          </div>
        }
        <div className="card-text-container">
          <div className="card-title" title={title}>
            {title}
          </div>
          <div className="card-description" title={description}>
            {description}
          </div>
        </div>
        {button && (
          <button 
            className={`card-button ${clicked || isInCollection ? 'card-button--clicked' : ''}`} 
            onClick={handleClick}
            disabled={!isAuthenticated}
          >
            {buttonIconUrl && <img src={buttonImg} alt="icon" className="button-icon" />}
            {buttonLbl}
          </button>
        )}
      </div>
    </div>
  );
}

export default CardElement;