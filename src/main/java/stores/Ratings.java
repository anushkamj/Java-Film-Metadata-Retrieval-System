package stores;

import java.time.LocalDateTime;

import interfaces.IRatings;
import structures.*;

public class Ratings implements IRatings {
    Stores stores;

    /**
     * Inner class to hold a single rating entry.
     */
    private class RatingData {
        int userID;
        int movieID;
        float rating;
        LocalDateTime timestamp;

        RatingData(int userID, int movieID, float rating, LocalDateTime timestamp) {
            this.userID = userID;
            this.movieID = movieID;
            this.rating = rating;
            this.timestamp = timestamp;
        }
    }

    // Primary store: composite key "userID:movieID" -> RatingData
    private MyHashMap<Long, RatingData> ratings;

    // Index: movieID -> list of composite keys for that movie
    private MyHashMap<Integer, MyList<Long>> movieIndex;

    // Index: userID -> list of composite keys for that user
    private MyHashMap<Integer, MyList<Long>> userIndex;

    // Total number of ratings
    private int totalSize;

    /**
     * The constructor for the Ratings data store. Initialises the primary
     * hash map and two index hash maps for efficient lookups.
     *
     * @param stores An object storing all the different key stores,
     *               including itself
     */
    public Ratings(Stores stores) {
        this.stores = stores;
        this.ratings = new MyHashMap<>(32768);
        this.movieIndex = new MyHashMap<>(2048);
        this.userIndex = new MyHashMap<>(4096);
        this.totalSize = 0;
    }

    /**
     * Creates a composite key from userID and movieID.
     * Uses bit shifting to create a unique long from two ints.
     *
     * @param userID  The user ID
     * @param movieID The movie ID
     * @return A unique long key combining both IDs
     */
    private long makeKey(int userID, int movieID) {
        return ((long) userID << 32) | (movieID & 0xFFFFFFFFL);
    }

    /**
     * Adds a rating to the data structure. The rating is made unique by its user ID
     * and its movie ID. Rejects duplicates.
     *
     * @param userid    The user ID
     * @param movieid   The movie ID
     * @param rating    The rating gave to the film by this user (between 0 and 5 inclusive)
     * @param timestamp The time at which the rating was made
     * @return TRUE if the data able to be added, FALSE otherwise
     */
    @Override
    public boolean add(int userid, int movieid, float rating, LocalDateTime timestamp) {
        long key = makeKey(userid, movieid);

        // Reject if this user+movie combination already exists
        if (ratings.containsKey(key)) {
            return false;
        }

        ratings.put(key, new RatingData(userid, movieid, rating, timestamp));

        // Update movie index
        MyList<Long> movieKeys = movieIndex.get(movieid);
        if (movieKeys == null) {
            movieKeys = new MyList<>(8);
            movieIndex.put(movieid, movieKeys);
        }
        movieKeys.add(key);

        // Update user index
        MyList<Long> userKeys = userIndex.get(userid);
        if (userKeys == null) {
            userKeys = new MyList<>(8);
            userIndex.put(userid, userKeys);
        }
        userKeys.add(key);

        totalSize++;
        return true;
    }

    /**
     * Removes a given rating, using the user ID and the movie ID as the unique identifier.
     *
     * @param userid  The user ID
     * @param movieid The movie ID
     * @return TRUE if the data was removed successfully, FALSE otherwise
     */
    @Override
    public boolean remove(int userid, int movieid) {
        long key = makeKey(userid, movieid);

        if (!ratings.containsKey(key)) {
            return false;
        }

        ratings.remove(key);

        // Remove from movie index
        MyList<Long> movieKeys = movieIndex.get(movieid);
        if (movieKeys != null) {
            for (int i = 0; i < movieKeys.size(); i++) {
                if (movieKeys.get(i) == key) {
                    movieKeys.removeAt(i);
                    break;
                }
            }
            if (movieKeys.isEmpty()) {
                movieIndex.remove(movieid);
            }
        }

        // Remove from user index
        MyList<Long> userKeys = userIndex.get(userid);
        if (userKeys != null) {
            for (int i = 0; i < userKeys.size(); i++) {
                if (userKeys.get(i) == key) {
                    userKeys.removeAt(i);
                    break;
                }
            }
            if (userKeys.isEmpty()) {
                userIndex.remove(userid);
            }
        }

        totalSize--;
        return true;
    }

    /**
     * Sets a rating for a given user ID and movie ID. If the user has already
     * rated the movie, the existing rating is overwritten. Otherwise, a new
     * rating is added.
     *
     * @param userid    The user ID
     * @param movieid   The movie ID
     * @param rating    The new rating (between 0 and 5 inclusive)
     * @param timestamp The time at which the new rating was made
     * @return TRUE if the data able to be added/updated, FALSE otherwise
     */
    @Override
    public boolean set(int userid, int movieid, float rating, LocalDateTime timestamp) {
        long key = makeKey(userid, movieid);

        if (ratings.containsKey(key)) {
            // Overwrite existing rating
            RatingData existing = ratings.get(key);
            existing.rating = rating;
            existing.timestamp = timestamp;
            return true;
        } else {
            // Add new rating
            return add(userid, movieid, rating, timestamp);
        }
    }

    /**
     * Get all the ratings for a given film.
     *
     * @param movieid The movie ID
     * @return An array of ratings, or an empty array if none found
     */
    @Override
    public float[] getMovieRatings(int movieid) {
        MyList<Long> movieKeys = movieIndex.get(movieid);
        if (movieKeys == null || movieKeys.isEmpty()) {
            return new float[0];
        }

        float[] result = new float[movieKeys.size()];
        for (int i = 0; i < movieKeys.size(); i++) {
            RatingData rd = ratings.get(movieKeys.get(i));
            result[i] = rd.rating;
        }
        return result;
    }

    /**
     * Get all the ratings for a given user.
     *
     * @param userid The user ID
     * @return An array of ratings, or an empty array if none found
     */
    @Override
    public float[] getUserRatings(int userid) {
        MyList<Long> userKeys = userIndex.get(userid);
        if (userKeys == null || userKeys.isEmpty()) {
            return new float[0];
        }

        float[] result = new float[userKeys.size()];
        for (int i = 0; i < userKeys.size(); i++) {
            RatingData rd = ratings.get(userKeys.get(i));
            result[i] = rd.rating;
        }
        return result;
    }

    /**
     * Get the average rating for a given film.
     * If film is in Ratings, returns the average.
     * If film is not in Ratings but exists in Movies store, returns 0.0f.
     * If film is not in Ratings or Movies store, returns -1.0f.
     *
     * @param movieid The movie ID
     * @return The average rating for the film
     */
    @Override
    public float getMovieAverageRating(int movieid) {
        MyList<Long> movieKeys = movieIndex.get(movieid);

        if (movieKeys != null && !movieKeys.isEmpty()) {
            // Film has ratings — compute average
            float sum = 0;
            for (int i = 0; i < movieKeys.size(); i++) {
                RatingData rd = ratings.get(movieKeys.get(i));
                sum += rd.rating;
            }
            return sum / movieKeys.size();
        }

        // No ratings for this movie — check if movie exists in Movies store
        if (stores.getMovies() != null && stores.getMovies().getTitle(movieid) != null) {
            return 0.0f;
        }

        return -1.0f;
    }

    /**
     * Get the average rating for a given user.
     * If the user cannot be found in Ratings or there are no ratings, returns -1.0f.
     *
     * @param userid The user ID
     * @return The average rating for the user
     */
    @Override
    public float getUserAverageRating(int userid) {
        MyList<Long> userKeys = userIndex.get(userid);

        if (userKeys == null || userKeys.isEmpty()) {
            return -1.0f;
        }

        float sum = 0;
        for (int i = 0; i < userKeys.size(); i++) {
            RatingData rd = ratings.get(userKeys.get(i));
            sum += rd.rating;
        }
        return sum / userKeys.size();
    }

    /**
     * Gets the top N movies with the most ratings, in order from most to least.
     *
     * @param num The number of movies that should be returned
     * @return A sorted array of movie IDs with the most ratings
     */
    @Override
    public int[] getMostRatedMovies(int num) {
        Object[] keys = movieIndex.getKeys();
        int count = keys.length;

        if (count == 0 || num == 0) {
            return new int[0];
        }

        // Build parallel arrays of movieID and rating count
        int[] movieIDs = new int[count];
        int[] counts = new int[count];
        for (int i = 0; i < count; i++) {
            movieIDs[i] = (Integer) keys[i];
            counts[i] = movieIndex.get(movieIDs[i]).size();
        }

        // Sort by count descending using insertion sort
        for (int i = 1; i < count; i++) {
            int tmpID = movieIDs[i];
            int tmpCount = counts[i];
            int j = i - 1;
            while (j >= 0 && counts[j] < tmpCount) {
                counts[j + 1] = counts[j];
                movieIDs[j + 1] = movieIDs[j];
                j--;
            }
            counts[j + 1] = tmpCount;
            movieIDs[j + 1] = tmpID;
        }

        int resultSize = num < count ? num : count;
        int[] result = new int[resultSize];
        for (int i = 0; i < resultSize; i++) {
            result[i] = movieIDs[i];
        }
        return result;
    }

    /**
     * Gets the top N users with the most ratings, in order from most to least.
     *
     * @param num The number of users that should be returned
     * @return A sorted array of user IDs with the most ratings
     */
    @Override
    public int[] getMostRatedUsers(int num) {
        Object[] keys = userIndex.getKeys();
        int count = keys.length;

        if (count == 0 || num == 0) {
            return new int[0];
        }

        // Build parallel arrays of userID and rating count
        int[] userIDs = new int[count];
        int[] counts = new int[count];
        for (int i = 0; i < count; i++) {
            userIDs[i] = (Integer) keys[i];
            counts[i] = userIndex.get(userIDs[i]).size();
        }

        // Sort by count descending using insertion sort
        for (int i = 1; i < count; i++) {
            int tmpID = userIDs[i];
            int tmpCount = counts[i];
            int j = i - 1;
            while (j >= 0 && counts[j] < tmpCount) {
                counts[j + 1] = counts[j];
                userIDs[j + 1] = userIDs[j];
                j--;
            }
            counts[j + 1] = tmpCount;
            userIDs[j + 1] = tmpID;
        }

        int resultSize = num < count ? num : count;
        int[] result = new int[resultSize];
        for (int i = 0; i < resultSize; i++) {
            result[i] = userIDs[i];
        }
        return result;
    }

    /**
     * Get the number of ratings that a movie has.
     * If the movie exists in Ratings, returns the count.
     * If the movie exists in Movies store but has no ratings, returns 0.
     * If the movie does not exist in either store, returns -1.
     *
     * @param movieid The movie ID to be found
     * @return The number of ratings for the specified movie
     */
    @Override
    public int getNumRatings(int movieid) {
        MyList<Long> movieKeys = movieIndex.get(movieid);

        if (movieKeys != null && !movieKeys.isEmpty()) {
            return movieKeys.size();
        }

        // No ratings — check if movie exists in Movies store
        if (stores.getMovies() != null && stores.getMovies().getTitle(movieid) != null) {
            return 0;
        }

        return -1;
    }

    /**
     * Get the highest average rated film IDs, in order of their average rating
     * (highest first).
     *
     * @param numResults The maximum number of results to be returned
     * @return An array of film IDs with the highest average ratings, highest first
     */
    @Override
    public int[] getTopAverageRatedMovies(int numResults) {
        Object[] keys = movieIndex.getKeys();
        int count = keys.length;

        if (count == 0 || numResults == 0) {
            return new int[0];
        }

        // Build parallel arrays of movieID and average rating
        int[] movieIDs = new int[count];
        float[] averages = new float[count];
        for (int i = 0; i < count; i++) {
            movieIDs[i] = (Integer) keys[i];
            MyList<Long> movieKeys = movieIndex.get(movieIDs[i]);
            float sum = 0;
            for (int j = 0; j < movieKeys.size(); j++) {
                RatingData rd = ratings.get(movieKeys.get(j));
                sum += rd.rating;
            }
            averages[i] = sum / movieKeys.size();
        }

        // Sort by average descending using insertion sort
        for (int i = 1; i < count; i++) {
            int tmpID = movieIDs[i];
            float tmpAvg = averages[i];
            int j = i - 1;
            while (j >= 0 && averages[j] < tmpAvg) {
                averages[j + 1] = averages[j];
                movieIDs[j + 1] = movieIDs[j];
                j--;
            }
            averages[j + 1] = tmpAvg;
            movieIDs[j + 1] = tmpID;
        }

        int resultSize = numResults < count ? numResults : count;
        int[] result = new int[resultSize];
        for (int i = 0; i < resultSize; i++) {
            result[i] = movieIDs[i];
        }
        return result;
    }

    /**
     * Gets the number of ratings in the data structure.
     *
     * @return The number of ratings in the data structure
     */
    @Override
    public int size() {
        return totalSize;
    }
}
