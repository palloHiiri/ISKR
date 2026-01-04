import { useState, useEffect } from 'react';
import PrimaryButton from '../primary-button/PrimaryButton';
import SecondaryButton from '../secondary-button/SecondaryButton';
import Input from '../input/Input';
import './GenresSelectModal.scss';
import searchAPI from '../../../api/searchService';

interface Genre {
  genreId: number;
  name: string;
}

interface GenresSelectModalProps {
  onSelect: (genres: Genre[]) => void;
  onClose: () => void;
  goBack: () => void;
  initialSelected?: Genre[];
}

function GenresSelectModal({ onSelect, onClose, goBack, initialSelected = [] }: GenresSelectModalProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [selectedGenres, setSelectedGenres] = useState<Genre[]>(initialSelected);
  const [searchLoading, setSearchLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!searchQuery.trim()) {
      setSearchResults([]);
      return;
    }

    const searchGenres = async () => {
      setSearchLoading(true);
      try {
        const genres = await searchAPI.searchGenres(searchQuery.trim());
        setSearchResults(genres);
      } catch (err: any) {
        console.error('Error searching genres:', err);
        setSearchResults([]);
      } finally {
        setSearchLoading(false);
      }
    };

    const timeoutId = setTimeout(searchGenres, 300);
    return () => clearTimeout(timeoutId);
  }, [searchQuery]);

  const handleAddGenre = (genre: any) => {
    if (!selectedGenres.some(g => g.genreId === genre.id)) {
      setSelectedGenres([
        ...selectedGenres,
        {
          genreId: genre.id,
          name: genre.name
        }
      ]);
    }
  };

  const handleRemoveGenre = (genreId: number) => {
    setSelectedGenres(selectedGenres.filter(g => g.genreId !== genreId));
  };

  const handleSave = () => {
    if (selectedGenres.length === 0) {
      setError('Выберите хотя бы один жанр');
      return;
    }
    onSelect(selectedGenres);
  };

  return (
    <div className="genres-select-modal">
      <div className="modal-header">
        <button className="back-button" onClick={goBack}>
          ← Назад
        </button>
        <h2 className="modal-title">Выбор жанров</h2>
      </div>

      <div className="modal-content">
        <div className="selected-genres-section">
          <h3 className="section-title">Выбранные жанры</h3>
          {selectedGenres.length > 0 ? (
            <div className="genres-list">
              {selectedGenres.map(genre => (
                <div key={genre.genreId} className="genre-item">
                  <span className="genre-name">{genre.name}</span>
                  <button
                    className="remove-genre-btn"
                    onClick={() => handleRemoveGenre(genre.genreId)}
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <p className="no-genres-message">Жанры не выбраны</p>
          )}
        </div>

        <div className="search-section">
          <h3 className="section-title">Поиск жанров</h3>
          <Input
            type="text"
            placeholder="Начните вводить название жанра..."
            value={searchQuery}
            onChange={setSearchQuery}
          />
          
          {searchLoading ? (
            <div className="search-loading">Поиск...</div>
          ) : searchResults.length > 0 ? (
            <div className="search-results">
              {searchResults
                .filter(genre => !selectedGenres.some(g => g.genreId === genre.id))
                .map(genre => (
                  <div
                    key={genre.id}
                    className="search-result-item"
                    onClick={() => handleAddGenre(genre)}
                  >
                    <span className="genre-name">{genre.name}</span>
                    <div className="add-genre-btn">+</div>
                  </div>
                ))}
            </div>
          ) : searchQuery.trim() ? (
            <p className="no-results-message">Жанры не найдены</p>
          ) : null}
        </div>

        {error && (
          <div className="error-message">
            <span className="error-icon">⚠</span>
            <span>{error}</span>
          </div>
        )}

        <div className="modal-actions">
          <SecondaryButton
            label="Отмена"
            onClick={onClose}
          />
          <PrimaryButton
            label="Выбрать"
            onClick={handleSave}
          />
        </div>
      </div>
    </div>
  );
}

export default GenresSelectModal;