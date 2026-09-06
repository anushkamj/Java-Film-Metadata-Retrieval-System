package stores;

import structures.*;

import interfaces.ICredits;

public class Credits implements ICredits {
    Stores stores;

    /**
     * Inner class to hold cast and crew arrays for a single film.
     */
    private class FilmCredits {
        CastCredit[] cast;
        CrewCredit[] crew;

        FilmCredits(CastCredit[] cast, CrewCredit[] crew) {
            this.cast = cast;
            this.crew = crew;
        }
    }

    // Primary store: filmID -> FilmCredits (cast + crew arrays for that film)
    private MyHashMap<Integer, FilmCredits> filmCredits;

    // Person indexes: personID -> Person object (unique cast and crew)
    private MyHashMap<Integer, Person> castPersonMap;
    private MyHashMap<Integer, Person> crewPersonMap;

    // Person-to-films indexes: personID -> list of filmIDs they appeared in
    private MyHashMap<Integer, MyList<Integer>> castToFilms;
    private MyHashMap<Integer, MyList<Integer>> crewToFilms;

    // Cast credit count: personID -> total number of CastCredit entries across all films
    // (counts multiple roles in same film as separate credits)
    private MyHashMap<Integer, Integer> castCreditCount;

    /**
     * The constructor for the Credits data store. Initialises all hash maps
     * used for efficient lookups by film ID, cast ID, and crew ID.
     *
     * @param stores An object storing all the different key stores,
     *               including itself
     */
    public Credits(Stores stores) {
        this.stores = stores;
        this.filmCredits = new MyHashMap<>(2048);
        this.castPersonMap = new MyHashMap<>(16384);
        this.crewPersonMap = new MyHashMap<>(16384);
        this.castToFilms = new MyHashMap<>(16384);
        this.crewToFilms = new MyHashMap<>(16384);
        this.castCreditCount = new MyHashMap<>(16384);
    }

    /**
     * Adds data about the people who worked on a given film. The movie ID should be
     * unique. Also updates all person-level indexes.
     *
     * @param cast An array of all cast members that starred in the given film
     * @param crew An array of all crew members that worked on a given film
     * @param id   The (unique) movie ID
     * @return TRUE if the data able to be added, FALSE otherwise
     */
    @Override
    public boolean add(CastCredit[] cast, CrewCredit[] crew, int id) {
        // Reject duplicate film IDs
        if (filmCredits.containsKey(id)) {
            return false;
        }

        // Sort cast by order field before storing
        sortCastByOrder(cast);
        // Sort crew by id field before storing
        sortCrewByID(crew);

        filmCredits.put(id, new FilmCredits(cast, crew));

        // Update cast indexes
        for (int i = 0; i < cast.length; i++) {
            int castID = cast[i].getID();

            // Register person if not seen before
            if (!castPersonMap.containsKey(castID)) {
                castPersonMap.put(castID, new Person(castID, cast[i].getName(), cast[i].getProfilePath()));
            }

            // Add film to this cast member's film list (avoid duplicate filmIDs)
            MyList<Integer> films = castToFilms.get(castID);
            if (films == null) {
                films = new MyList<>(4);
                castToFilms.put(castID, films);
            }
            if (!films.contains(id)) {
                films.add(id);
            }

            // Increment credit count (each CastCredit entry = 1 credit, even if same film)
            Integer count = castCreditCount.get(castID);
            castCreditCount.put(castID, (count == null ? 0 : count) + 1);
        }

        // Update crew indexes
        for (int i = 0; i < crew.length; i++) {
            int crewID = crew[i].getID();

            if (!crewPersonMap.containsKey(crewID)) {
                crewPersonMap.put(crewID, new Person(crewID, crew[i].getName(), crew[i].getProfilePath()));
            }

            MyList<Integer> films = crewToFilms.get(crewID);
            if (films == null) {
                films = new MyList<>(4);
                crewToFilms.put(crewID, films);
            }
            if (!films.contains(id)) {
                films.add(id);
            }
        }

        return true;
    }

    /**
     * Remove a given films data from the data structure.
     * Also updates all person-level indexes accordingly.
     *
     * @param id The movie ID
     * @return TRUE if the data was removed, FALSE otherwise
     */
    @Override
    public boolean remove(int id) {
        FilmCredits fc = filmCredits.get(id);
        if (fc == null) {
            return false;
        }

        // Remove cast index entries for this film
        for (int i = 0; i < fc.cast.length; i++) {
            int castID = fc.cast[i].getID();

            // Decrement credit count
            Integer count = castCreditCount.get(castID);
            if (count != null) {
                int newCount = count - 1;
                if (newCount <= 0) {
                    castCreditCount.remove(castID);
                } else {
                    castCreditCount.put(castID, newCount);
                }
            }

            // Remove film from cast member's film list
            MyList<Integer> films = castToFilms.get(castID);
            if (films != null) {
                for (int j = 0; j < films.size(); j++) {
                    if (films.get(j) == id) {
                        films.removeAt(j);
                        break;
                    }
                }
                // If person has no more films, remove from indexes
                if (films.isEmpty()) {
                    castToFilms.remove(castID);
                    castPersonMap.remove(castID);
                }
            }
        }

        // Remove crew index entries for this film
        for (int i = 0; i < fc.crew.length; i++) {
            int crewID = fc.crew[i].getID();

            MyList<Integer> films = crewToFilms.get(crewID);
            if (films != null) {
                for (int j = 0; j < films.size(); j++) {
                    if (films.get(j) == id) {
                        films.removeAt(j);
                        break;
                    }
                }
                if (films.isEmpty()) {
                    crewToFilms.remove(crewID);
                    crewPersonMap.remove(crewID);
                }
            }
        }

        filmCredits.remove(id);
        return true;
    }

    /**
     * Gets all the cast members for a given film, sorted by their "order" field.
     *
     * @param filmID The movie ID
     * @return An array of CastCredit objects in "order" order. If the film cannot
     *         be found or has no cast, return an empty array
     */
    @Override
    public CastCredit[] getFilmCast(int filmID) {
        FilmCredits fc = filmCredits.get(filmID);
        if (fc == null || fc.cast == null) {
            return new CastCredit[0];
        }
        return fc.cast;
    }

    /**
     * Gets all the crew members for a given film, sorted by their "id" field.
     *
     * @param filmID The movie ID
     * @return An array of CrewCredit objects in "id" order. If the film cannot
     *         be found or has no crew, return an empty array
     */
    @Override
    public CrewCredit[] getFilmCrew(int filmID) {
        FilmCredits fc = filmCredits.get(filmID);
        if (fc == null || fc.crew == null) {
            return new CrewCredit[0];
        }
        return fc.crew;
    }

    /**
     * Gets the number of cast that worked on a given film.
     *
     * @param filmID The movie ID
     * @return The number of cast members, or -1 if the film cannot be found
     */
    @Override
    public int sizeOfCast(int filmID) {
        FilmCredits fc = filmCredits.get(filmID);
        if (fc == null) {
            return -1;
        }
        return fc.cast.length;
    }

    /**
     * Gets the number of crew that worked on a given film.
     *
     * @param filmID The movie ID
     * @return The number of crew members, or -1 if the film cannot be found
     */
    @Override
    public int sizeOfCrew(int filmID) {
        FilmCredits fc = filmCredits.get(filmID);
        if (fc == null) {
            return -1;
        }
        return fc.crew.length;
    }

    /**
     * Gets a list of all unique cast members present in the data structure.
     *
     * @return An array of all unique cast members as Person objects, or an empty array
     */
    @Override
    public Person[] getUniqueCast() {
        Object[] values = castPersonMap.getValues();
        Person[] result = new Person[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (Person) values[i];
        }
        return result;
    }

    /**
     * Gets a list of all unique crew members present in the data structure.
     *
     * @return An array of all unique crew members as Person objects, or an empty array
     */
    @Override
    public Person[] getUniqueCrew() {
        Object[] values = crewPersonMap.getValues();
        Person[] result = new Person[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (Person) values[i];
        }
        return result;
    }

    /**
     * Get all the cast members that have the given string within their name.
     *
     * @param cast The string that needs to be found
     * @return An array of unique Person objects matching the search, or an empty array
     */
    @Override
    public Person[] findCast(String cast) {
        MyList<Person> result = new MyList<>();
        Object[] values = castPersonMap.getValues();

        for (int i = 0; i < values.length; i++) {
            Person p = (Person) values[i];
            if (p.getName() != null && p.getName().contains(cast)) {
                result.add(p);
            }
        }

        Person[] arr = new Person[result.size()];
        for (int i = 0; i < result.size(); i++) {
            arr[i] = result.get(i);
        }
        return arr;
    }

    /**
     * Get all the crew members that have the given string within their name.
     *
     * @param crew The string that needs to be found
     * @return An array of unique Person objects matching the search, or an empty array
     */
    @Override
    public Person[] findCrew(String crew) {
        MyList<Person> result = new MyList<>();
        Object[] values = crewPersonMap.getValues();

        for (int i = 0; i < values.length; i++) {
            Person p = (Person) values[i];
            if (p.getName() != null && p.getName().contains(crew)) {
                result.add(p);
            }
        }

        Person[] arr = new Person[result.size()];
        for (int i = 0; i < result.size(); i++) {
            arr[i] = result.get(i);
        }
        return arr;
    }

    /**
     * Gets the Person object corresponding to the cast ID.
     *
     * @param castID The cast ID of the person to be found
     * @return The Person object, or null if not found
     */
    @Override
    public Person getCast(int castID) {
        return castPersonMap.get(castID);
    }

    /**
     * Gets the Person object corresponding to the crew ID.
     *
     * @param crewID The crew ID of the person to be found
     * @return The Person object, or null if not found
     */
    @Override
    public Person getCrew(int crewID) {
        return crewPersonMap.get(crewID);
    }

    /**
     * Get an array of film IDs where the cast member has starred in.
     *
     * @param castID The cast ID of the person
     * @return An array of film IDs, or an empty array if none found
     */
    @Override
    public int[] getCastFilms(int castID) {
        MyList<Integer> films = castToFilms.get(castID);
        if (films == null || films.isEmpty()) {
            return new int[0];
        }
        int[] result = new int[films.size()];
        for (int i = 0; i < films.size(); i++) {
            result[i] = films.get(i);
        }
        return result;
    }

    /**
     * Get an array of film IDs where the crew member has worked on.
     *
     * @param crewID The crew ID of the person
     * @return An array of film IDs, or an empty array if none found
     */
    @Override
    public int[] getCrewFilms(int crewID) {
        MyList<Integer> films = crewToFilms.get(crewID);
        if (films == null || films.isEmpty()) {
            return new int[0];
        }
        int[] result = new int[films.size()];
        for (int i = 0; i < films.size(); i++) {
            result[i] = films.get(i);
        }
        return result;
    }

    /**
     * Get the films that this cast member stars in (top 3 billing, i.e. order 0, 1, or 2).
     *
     * @param castID The cast ID of the cast member to be searched for
     * @return An array of film IDs where the cast member stars in (top 3), or empty array
     */
    @Override
    public int[] getCastStarsInFilms(int castID) {
        MyList<Integer> films = castToFilms.get(castID);
        if (films == null || films.isEmpty()) {
            return new int[0];
        }

        MyList<Integer> result = new MyList<>();
        for (int i = 0; i < films.size(); i++) {
            int filmID = films.get(i);
            FilmCredits fc = filmCredits.get(filmID);
            if (fc != null) {
                for (int j = 0; j < fc.cast.length; j++) {
                    // Top 3 billing = order values 1, 2, or 3 (1-indexed)
                    if (fc.cast[j].getID() == castID && fc.cast[j].getOrder() <= 3) {
                        result.add(filmID);
                        break; // Only count this film once
                    }
                }
            }
        }

        int[] arr = new int[result.size()];
        for (int i = 0; i < result.size(); i++) {
            arr[i] = result.get(i);
        }
        return arr;
    }

    /**
     * Get Person objects for cast members who have appeared in the most films.
     * Multiple roles in the same film count as separate credits.
     * Ordered highest to lowest number of credits.
     *
     * @param numResults The maximum number of elements that should be returned
     * @return An array of Person objects ordered by highest credits
     */
    @Override
    public Person[] getMostCastCredits(int numResults) {
        Object[] keys = castCreditCount.getKeys();
        int count = keys.length;

        if (count == 0) {
            return new Person[0];
        }

        // Build parallel arrays of personID and credit count
        int[] personIDs = new int[count];
        int[] counts = new int[count];
        for (int i = 0; i < count; i++) {
            personIDs[i] = (Integer) keys[i];
            counts[i] = castCreditCount.get(personIDs[i]);
        }

        // Sort by credit count descending using insertion sort
        for (int i = 1; i < count; i++) {
            int tmpID = personIDs[i];
            int tmpCount = counts[i];
            int j = i - 1;
            while (j >= 0 && counts[j] < tmpCount) {
                counts[j + 1] = counts[j];
                personIDs[j + 1] = personIDs[j];
                j--;
            }
            counts[j + 1] = tmpCount;
            personIDs[j + 1] = tmpID;
        }

        // Take top numResults (or fewer if not enough cast members)
        int resultSize = numResults < count ? numResults : count;
        Person[] result = new Person[resultSize];
        for (int i = 0; i < resultSize; i++) {
            result[i] = castPersonMap.get(personIDs[i]);
        }
        return result;
    }

    /**
     * Get the number of credits for a given cast member. Multiple roles in the
     * same film count as separate credits.
     *
     * @param castID A cast ID representing the cast member to be found
     * @return The number of credits, or -1 if the cast member cannot be found
     */
    @Override
    public int getNumCastCredits(int castID) {
        Integer count = castCreditCount.get(castID);
        return count != null ? count : -1;
    }

    /**
     * Gets the number of films stored in this data structure.
     *
     * @return The number of films in the data structure
     */
    @Override
    public int size() {
        return filmCredits.size();
    }

    // ========================== SORTING HELPERS ==========================

    /**
     * Sorts a CastCredit array by the "order" field in ascending order
     * using insertion sort (efficient for small arrays, stable).
     *
     * @param cast The array to sort in place
     */
    private void sortCastByOrder(CastCredit[] cast) {
        for (int i = 1; i < cast.length; i++) {
            CastCredit key = cast[i];
            int j = i - 1;
            while (j >= 0 && cast[j].getOrder() > key.getOrder()) {
                cast[j + 1] = cast[j];
                j--;
            }
            cast[j + 1] = key;
        }
    }

    /**
     * Sorts a CrewCredit array by the "id" field (getID()) in ascending order
     * using insertion sort (efficient for small arrays, stable).
     *
     * @param crew The array to sort in place
     */
    private void sortCrewByID(CrewCredit[] crew) {
        for (int i = 1; i < crew.length; i++) {
            CrewCredit key = crew[i];
            int j = i - 1;
            while (j >= 0 && crew[j].getID() > key.getID()) {
                crew[j + 1] = crew[j];
                j--;
            }
            crew[j + 1] = key;
        }
    }
}
