package com.election.loader;

import com.election.exception.DataLoadException;

import java.nio.file.Path;
import java.util.List;

/** Turns one data file into objects of type T. Every CSV loader implements it. */
public interface DataLoader<T> {

    List<T> load(Path path) throws DataLoadException;
}
