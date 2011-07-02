package jmri.jmrit.operations.locations;

/**
 * An Object representing a location and track.
 * @author Daniel Boudreau Copyright (C) 2009
 *
 */
public class LocationTrackPair {
	Location _location;
	Track _track;
	
	public LocationTrackPair(Location location, Track track){
		_location = location;
		_track = track;
	}
	
	// for combo boxes
	public String toString(){
		return _location.getName()+" ("+_track.getName()+")";
	}
	
	public Track getTrack(){
		return _track;
	}
	
	public Location getLocation(){
		return _location;
	}

}

