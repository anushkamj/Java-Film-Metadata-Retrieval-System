package stores;

import java.time.LocalDate;

import interfaces.IMovies;
import structures.*;

public class Movies implements IMovies {
    Stores stores;

    /**
     * Inner class to hold all data fields for a single film.
     * Bundling these together means one hash map lookup retrieves everything.
     */
    private class FilmData {
        int id;
        String title;
        String originalTitle;
        String overview;
        String tagline;
        String status;
        Genre[] genres;
        LocalDate release;
        long budget;
        long revenue;
        String[] languages;
        String originalLanguage;
        double runtime;
        String homepage;
        boolean adult;
        boolean video;
        String poster;

        // Fields set later via setter methods
        String imdbID;
        double popularity;
        double voteAverage;
        int voteCount;
        int collectionID;       // -1 if not in a collection

        // Dynamic lists for production companies and countries
        MyList<Company> productionCompanies;
        MyList<String> productionCountries;

        FilmData(int id, String title, String originalTitle, String overview,
                 String tagline, String status, Genre[] genres, LocalDate release,
                 long budget, long revenue, String[] languages, String originalLanguage,
                 double runtime, String homepage, boolean adult, boolean video, String poster) {
            this.id = id;
            this.title = title;
            this.originalTitle = originalTitle;
            this.overview = overview;
            this.tagline = tagline;
            this.status = status;
            this.genres = genres;
            this.release = release;
            this.budget = budget;
            this.revenue = revenue;
            this.languages = languages;
            this.originalLanguage = originalLanguage;
            this.runtime = runtime;
            this.homepage = homepage;
            this.adult = adult;
            this.video = video;
            this.poster = poster;

            this.imdbID = null;
            this.popularity = 0.0;
            this.voteAverage = 0.0;
            this.voteCount = 0;
            this.collectionID = -1;

            this.productionCompanies = new MyList<>(4);
            this.productionCountries = new MyList<>(4);
        }
    }

    /**
     * Inner class to hold collection metadata.
     */
    private class CollectionData {
        int collectionID;
        String name;
        String posterPath;
        String backdropPath;
        MyList<Integer> filmIDs; // Films belonging to this collection

        CollectionData(int collectionID, String name, String posterPath, String backdropPath) {
            this.collectionID = collectionID;
            this.name = name;
            this.posterPath = posterPath;
            this.backdropPath = backdropPath;
            this.filmIDs = new MyList<>(4);
        }
    }

    // Primary data store: filmID -> FilmData
    private MyHashMap<Integer, FilmData> films;

    // Secondary data store: collectionID -> CollectionData
    private MyHashMap<Integer, CollectionData> collections;

    /**
     * The constructor for the Movies data store. This is where you should
     * initialise your data structures.
     * @param stores An object storing all the different key stores,
     *               including itself
     */
    public Movies(Stores stores) {
        this.stores = stores;
        this.films = new MyHashMap<>(2048);
        this.collections = new MyHashMap<>(64);
    }

    /**
     * Adds data about a film to the data structure
     *
     * @param id               The unique ID for the film
     * @param title            The English title of the film
     * @param originalTitle    The original language title of the film
     * @param overview         An overview of the film
     * @param tagline          The tagline for the film (empty string if there is no tagline)
     * @param status           Current status of the film
     * @param genres           An array of Genre objects related to the film
     * @param release          The release date for the film
     * @param budget           The budget of the film in US Dollars
     * @param revenue          The revenue of the film in US Dollars
     * @param languages        An array of ISO 639 language codes for the film
     * @param originalLanguage An ISO 639 language code for the original language of the film
     * @param runtime          The runtime of the film in minutes
     * @param homepage         The URL to the homepage of the film
     * @param adult            Whether the film is an adult film
     * @param video            Whether the film is a "direct-to-video" film
     * @param poster           The unique part of the URL of the poster
     * @return TRUE if the data able to be added, FALSE otherwise
     */
    @Override
    public boolean add(int id, String title, String originalTitle, String overview,
                       String tagline, String status, Genre[] genres, LocalDate release,
                       long budget, long revenue, String[] languages, String originalLanguage,
                       double runtime, String homepage, boolean adult, boolean video, String poster) {
        // Reject duplicate IDs
        if (films.containsKey(id)) {
            return false;
        }

        FilmData film = new FilmData(id, title, originalTitle, overview, tagline, status,
                genres, release, budget, revenue, languages, originalLanguage,
                runtime, homepage, adult, video, poster);
        films.put(id, film);
        return true;
    }

    /**
     * Removes a film from the data structure, and any data
     * added through this class related to the film
     *
     * @param id The film ID
     * @return TRUE if the film has been removed successfully, FALSE otherwise
     */
    @Override
    public boolean remove(int id) {
        if (!films.containsKey(id)) {
            return false;
        }

        FilmData film = films.get(id);

        // If film was in a collection, remove it from that collection's film list
        if (film.collectionID != -1) {
            CollectionData collection = collections.get(film.collectionID);
            if (collection != null) {
                for (int i = 0; i < collection.filmIDs.size(); i++) {
                    if (collection.filmIDs.get(i) == id) {
                        collection.filmIDs.removeAt(i);
                        break;
                    }
                }
                // If collection is now empty, remove it
                if (collection.filmIDs.isEmpty()) {
                    collections.remove(film.collectionID);
                }
            }
        }

        films.remove(id);
        return true;
    }

    /**
     * Gets all the IDs for all films
     *
     * @return An array of all film IDs stored
     */
    @Override
    public int[] getAllIDs() {
        Object[] keys = films.getKeys();
        int[] ids = new int[keys.length];
        for (int i = 0; i < keys.length; i++) {
            ids[i] = (Integer) keys[i];
        }
        return ids;
    }

    /**
     * Finds the film IDs of all films released within a given range. If a film is
     * released either on the start or end dates, then that film should not be included
     *
     * @param start The start point of the range of dates
     * @param end   The end point of the range of dates
     * @return An array of film IDs that were released between start and end
     */
    @Override
    public int[] getAllIDsReleasedInRange(LocalDate start, LocalDate end) {
        MyList<Integer> result = new MyList<>();
        Object[] keys = films.getKeys();

        for (int i = 0; i < keys.length; i++) {
            FilmData film = films.get((Integer) keys[i]);
            if (film.release != null && film.release.isAfter(start) && film.release.isBefore(end)) {
                result.add(film.id);
            }
        }

        int[] ids = new int[result.size()];
        for (int i = 0; i < result.size(); i++) {
            ids[i] = result.get(i);
        }
        return ids;
    }

    /**
     * Gets the title of a particular film, given the ID number of that film
     *
     * @param id The movie ID
     * @return The title of the requested film. If the film cannot be found, then return null
     */
    @Override
    public String getTitle(int id) {
        FilmData film = films.get(id);
        return film != null ? film.title : null;
    }

    /**
     * Gets the original title of a particular film, given the ID number of that film
     *
     * @param id The movie ID
     * @return The original title of the requested film. If the film cannot be found, then return null
     */
    @Override
    public String getOriginalTitle(int id) {
        FilmData film = films.get(id);
        return film != null ? film.originalTitle : null;
    }

    /**
     * Gets the overview of a particular film, given the ID number of that film
     *
     * @param id The movie ID
     * @return The overview of the requested film. If the film cannot be found, then return null
     */
    @Override
    public String getOverview(int id) {
        FilmData film = films.get(id);
        return film != null ? film.overview : null;
    }

    /**
     * Gets the tagline of a particular film, given the ID number of that film
     *
     * @param id The movie ID
     * @return The tagline of the requested film. If the film cannot be found, then return null
     */
    @Override
    public String getTagline(int id) {
        FilmData film = films.get(id);
        return film != null ? film.tagline : null;
    }

    /**
     * Gets the status of a particular film, given the ID number of that film
     *
     * @param id The movie ID
     * @return The status of the requested film. If the film cannot be found, then return null
     */
    @Override
    public String getStatus(int id) {
        FilmData film = films.get(id);
        return film != null ? film.status : null;
    }

    /**
     * Gets the genres of a particular film, given the ID number of that film
     *
     * @param id The movie ID
     * @return The genres of the requested film. If the film cannot be found, then return null
     */
    @Override
    public Genre[] getGenres(int id) {
        FilmData film = films.get(id);
        return film != null ? film.genres : null;
    }

    /**
     * Gets the release date of a particular film, given the ID number of that film
     *
     * @param id The movie ID
     * @return The release date of the requested film. If the film cannot be found, then return null
     */
    @Override
    public LocalDate getRelease(int id) {
        FilmData film = films.get(id);
        return film != null ? film.release : null;
    }

    /**
     * Gets the budget of a particular film, given the ID number of that film
     *
     * @param id The movie ID
     * @return The budget of the requested film. If the film cannot be found, then return -1
     */
    @Override
    public long getBudget(int id) {
        FilmData film = films.get(id);
        return film != null ? film.budget : -1;
    }

    /**
     * Gets the revenue of a particular film, given the ID number of that film
     *
     * @param id The movie ID
     * @return The revenue of the requested film. If the film cannot be found, then return -1
     */
    @Override
    public long getRevenue(int id) {
        FilmData film = films.get(id);
        return film != null ? film.revenue : -1;
    }

    /**
     * Gets the languages of a particular film, given the ID number of that film
     *
     * @param id The movie ID
     * @return The languages of the requested film. If the film cannot be found, then return null
     */
    @Override
    public String[] getLanguages(int id) {
        FilmData film = films.get(id);
        return film != null ? film.languages : null;
    }

    /**
     * Gets the original language of a particular film, given the ID number of that film
     *
     * @param id The movie ID
     * @return The original language of the requested film. If the film cannot be found, then return null
     */
    @Override
    public String getOriginalLanguage(int id) {
        FilmData film = films.get(id);
        return film != null ? film.originalLanguage : null;
    }

    /**
     * Gets the runtime of a particular film, given the ID number of that film
     *
     * @param id The movie ID
     * @return The runtime of the requested film. If the film cannot be found, then return -1.0d
     */
    @Override
    public double getRuntime(int id) {
        FilmData film = films.get(id);
        return film != null ? film.runtime : -1.0d;
    }

    /**
     * Gets the homepage of a particular film, given the ID number of that film
     *
     * @param id The movie ID
     * @return The homepage of the requested film. If the film cannot be found, then return null
     */
    @Override
    public String getHomepage(int id) {
        FilmData film = films.get(id);
        return film != null ? film.homepage : null;
    }

    /**
     * Gets weather a particular film is classed as "adult", given the ID number of that film
     *
     * @param id The movie ID
     * @return The "adult" status of the requested film. If the film cannot be found, then return false
     */
    @Override
    public boolean getAdult(int id) {
        FilmData film = films.get(id);
        return film != null ? film.adult : false;
    }

    /**
     * Gets weather a particular film is classed as "direct-to-video", given the ID number of that film
     *
     * @param id The movie ID
     * @return The "direct-to-video" status of the requested film. If the film cannot be found, then return false
     */
    @Override
    public boolean getVideo(int id) {
        FilmData film = films.get(id);
        return film != null ? film.video : false;
    }

    /**
     * Gets the poster URL of a particular film, given the ID number of that film
     *
     * @param id The movie ID
     * @return The poster URL of the requested film. If the film cannot be found, then return null
     */
    @Override
    public String getPoster(int id) {
        FilmData film = films.get(id);
        return film != null ? film.poster : null;
    }

    /**
     * Sets the average IMDb score and the number of reviews used to generate this
     * score, for a particular film
     *
     * @param id          The movie ID
     * @param voteAverage The average score on IMDb for the film
     * @param voteCount   The number of reviews on IMDb that were used to generate
     *                    the average score for the film
     * @return TRUE if the data able to be added, FALSE otherwise
     */
    @Override
    public boolean setVote(int id, double voteAverage, int voteCount) {
        FilmData film = films.get(id);
        if (film == null) {
            return false;
        }
        film.voteAverage = voteAverage;
        film.voteCount = voteCount;
        return true;
    }

    /**
     * Gets the average score for IMDb reviews of a particular film, given the ID number of that film
     *
     * @param id The movie ID
     * @return The average score for IMDb reviews of the requested film. If the film
     *         cannot be found, then return -1.0d
     */
    @Override
    public double getVoteAverage(int id) {
        FilmData film = films.get(id);
        return film != null ? film.voteAverage : -1.0d;
    }

    /**
     * Gets the amount of IMDb reviews used to generate the average score of a
     * particular film, given the ID number of that film
     *
     * @param id The movie ID
     * @return The amount of IMDb reviews used to generate the average score of the
     *         requested film. If the film cannot be found, then return -1
     */
    @Override
    public int getVoteCount(int id) {
        FilmData film = films.get(id);
        return film != null ? film.voteCount : -1;
    }

    /**
     * Adds a given film to a collection. The collection is required to have an ID
     * number, a name, and a URL to a poster for the collection
     *
     * @param filmID                 The movie ID
     * @param collectionID           The collection ID
     * @param collectionName         The name of the collection
     * @param collectionPosterPath   The URL where the poster can be found
     * @param collectionBackdropPath The URL where the backdrop can be found
     * @return TRUE if the data able to be added, FALSE otherwise
     */
    @Override
    public boolean addToCollection(int filmID, int collectionID, String collectionName,
                                   String collectionPosterPath, String collectionBackdropPath) {
        FilmData film = films.get(filmID);
        if (film == null) {
            return false;
        }

        // Link the film to this collection
        film.collectionID = collectionID;

        // Create the collection if it doesn't exist, or get the existing one
        CollectionData collection = collections.get(collectionID);
        if (collection == null) {
            collection = new CollectionData(collectionID, collectionName,
                    collectionPosterPath, collectionBackdropPath);
            collections.put(collectionID, collection);
        }

        // Add this film to the collection's list (avoid duplicates)
        if (!collection.filmIDs.contains(filmID)) {
            collection.filmIDs.add(filmID);
        }

        return true;
    }

    /**
     * Get all films that belong to a given collection
     *
     * @param collectionID The collection ID to be searched for
     * @return An array of film IDs that correspond to the given collection ID. If
     *         there are no films in the collection ID, or if the collection ID is
     *         not valid, return an empty array.
     */
    @Override
    public int[] getFilmsInCollection(int collectionID) {
        CollectionData collection = collections.get(collectionID);
        if (collection == null) {
            return new int[0];
        }

        int[] ids = new int[collection.filmIDs.size()];
        for (int i = 0; i < collection.filmIDs.size(); i++) {
            ids[i] = collection.filmIDs.get(i);
        }
        return ids;
    }

    /**
     * Gets the name of a given collection
     *
     * @param collectionID The collection ID
     * @return The name of the collection. If the collection cannot be found, then return null
     */
    @Override
    public String getCollectionName(int collectionID) {
        CollectionData collection = collections.get(collectionID);
        return collection != null ? collection.name : null;
    }

    /**
     * Gets the poster URL for a given collection
     *
     * @param collectionID The collection ID
     * @return The poster URL of the collection. If the collection cannot be found, then return null
     */
    @Override
    public String getCollectionPoster(int collectionID) {
        CollectionData collection = collections.get(collectionID);
        return collection != null ? collection.posterPath : null;
    }

    /**
     * Gets the backdrop URL for a given collection
     *
     * @param collectionID The collection ID
     * @return The backdrop URL of the collection. If the collection cannot be found, then return null
     */
    @Override
    public String getCollectionBackdrop(int collectionID) {
        CollectionData collection = collections.get(collectionID);
        return collection != null ? collection.backdropPath : null;
    }

    /**
     * Gets the collection ID of a given film
     *
     * @param filmID The movie ID
     * @return The collection ID for the requested film. If the film cannot be
     *         found, then return -1
     */
    @Override
    public int getCollectionID(int filmID) {
        FilmData film = films.get(filmID);
        return film != null ? film.collectionID : -1;
    }

    /**
     * Sets the IMDb ID for a given film
     *
     * @param filmID The movie ID
     * @param imdbID The IMDb ID
     * @return TRUE if the data able to be set, FALSE otherwise
     */
    @Override
    public boolean setIMDB(int filmID, String imdbID) {
        FilmData film = films.get(filmID);
        if (film == null) {
            return false;
        }
        film.imdbID = imdbID;
        return true;
    }

    /**
     * Gets the IMDb ID for a given film
     *
     * @param filmID The movie ID
     * @return The IMDb ID for the requested film. If the film cannot be found, return null
     */
    @Override
    public String getIMDB(int filmID) {
        FilmData film = films.get(filmID);
        return film != null ? film.imdbID : null;
    }

    /**
     * Sets the popularity of a given film. If the popularity for a film already exists, replace it with the new value
     *
     * @param id         The movie ID
     * @param popularity The popularity of the film
     * @return TRUE if the data able to be set, FALSE otherwise
     */
    @Override
    public boolean setPopularity(int id, double popularity) {
        FilmData film = films.get(id);
        if (film == null) {
            return false;
        }
        film.popularity = popularity;
        return true;
    }

    /**
     * Gets the popularity of a given film
     *
     * @param id The movie ID
     * @return The popularity value of the requested film. If the film cannot be
     *         found, then return -1.0d. If the popularity has not been set, return 0.0
     */
    @Override
    public double getPopularity(int id) {
        FilmData film = films.get(id);
        if (film == null) {
            return -1.0d;
        }
        return film.popularity; // Defaults to 0.0 if never set
    }

    /**
     * Adds a production company to a given film
     *
     * @param id      The movie ID
     * @param company A Company object that represents the details on a production company
     * @return TRUE if the data able to be added, FALSE otherwise
     */
    @Override
    public boolean addProductionCompany(int id, Company company) {
        FilmData film = films.get(id);
        if (film == null) {
            return false;
        }
        film.productionCompanies.add(company);
        return true;
    }

    /**
     * Adds a production country to a given film
     *
     * @param id      The movie ID
     * @param country A ISO 3166 string containing the 2-character country code
     * @return TRUE if the data able to be added, FALSE otherwise
     */
    @Override
    public boolean addProductionCountry(int id, String country) {
        FilmData film = films.get(id);
        if (film == null) {
            return false;
        }
        film.productionCountries.add(country);
        return true;
    }

    /**
     * Gets all the production companies for a given film
     *
     * @param id The movie ID
     * @return An array of Company objects that represent all the production
     *         companies that worked on the requested film. If the film cannot be
     *         found, then return null
     */
    @Override
    public Company[] getProductionCompanies(int id) {
        FilmData film = films.get(id);
        if (film == null) {
            return null;
        }
        Company[] companies = new Company[film.productionCompanies.size()];
        for (int i = 0; i < film.productionCompanies.size(); i++) {
            companies[i] = film.productionCompanies.get(i);
        }
        return companies;
    }

    /**
     * Gets all the production countries for a given film
     *
     * @param id The movie ID
     * @return An array of Strings that represent all the production countries (in
     *         ISO 3166 format) that worked on the requested film. If the film
     *         cannot be found, then return null
     */
    @Override
    public String[] getProductionCountries(int id) {
        FilmData film = films.get(id);
        if (film == null) {
            return null;
        }
        String[] countries = new String[film.productionCountries.size()];
        for (int i = 0; i < film.productionCountries.size(); i++) {
            countries[i] = film.productionCountries.get(i);
        }
        return countries;
    }

    /**
     * States the number of movies stored in the data structure
     *
     * @return The number of movies stored in the data structure
     */
    @Override
    public int size() {
        return films.size();
    }

    /**
     * Produces a list of movie IDs that have the search term in their title,
     * original title or their overview
     *
     * @param searchTerm The term that needs to be checked
     * @return An array of movie IDs that have the search term in their title,
     *         original title or their overview. If no movies have this search term,
     *         then an empty array should be returned
     */
    @Override
    public int[] findFilms(String searchTerm) {
        MyList<Integer> result = new MyList<>();
        Object[] keys = films.getKeys();

        for (int i = 0; i < keys.length; i++) {
            FilmData film = films.get((Integer) keys[i]);
            // Check if the search term appears in title, original title, or overview
            if ((film.title != null && film.title.contains(searchTerm)) ||
                (film.originalTitle != null && film.originalTitle.contains(searchTerm)) ||
                (film.overview != null && film.overview.contains(searchTerm))) {
                result.add(film.id);
            }
        }

        int[] ids = new int[result.size()];
        for (int i = 0; i < result.size(); i++) {
            ids[i] = result.get(i);
        }
        return ids;
    }
}
