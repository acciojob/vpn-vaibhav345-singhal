package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception {
        User user = userRepository2.findById(userId).get();

        if (user.getConnected()) {
            throw new Exception("Already connected");
        } else if (countryName.equalsIgnoreCase(user.getOriginalCountry().getCountryName().name())) {
            return user;
        } else {
            int smallestIdServiceProvider = Integer.MAX_VALUE;
            boolean isAvailableProvider = false;
            Country country1 = null;
            ServiceProvider serviceProvider1 = null;
            for (ServiceProvider serviceProvider : user.getServiceProviderList()) {
                for (Country country : serviceProvider.getCountryList()) {
                    if (countryName.equalsIgnoreCase(country.getCountryName().name())) {
                        if (smallestIdServiceProvider > serviceProvider.getId()) {
                            country1 = country;
                            serviceProvider1 = serviceProvider;
                            smallestIdServiceProvider = Math.min(serviceProvider.getId(), smallestIdServiceProvider);
                            isAvailableProvider = true;
                        }
                    }
                }
            }
            if (!isAvailableProvider) throw new Exception("Unable to connect");
            else {
                user.setConnected(true);

                user.setMaskedIp(country1.getCode() + "." + serviceProvider1.getId() + "." + userId);
            }
        }

        user = userRepository2.save(user);
        return user;
    }

    @Override
    public User disconnect(int userId) throws Exception {
        User user = userRepository2.findById(userId).get();
        if (!user.getConnected()) throw new Exception("Already disconnected");
        else {
            user.setConnected(false);
            user.setMaskedIp(null);
            user = userRepository2.save(user);
        }
        return user;
    }

    @Override
    public User communicate(int senderId, int receiverId) throws Exception {

        User receiver = userRepository2.findById(receiverId).get();

        User sender = userRepository2.findById(senderId).get();

        boolean isPossible = false;

        if (receiver.getConnected()) {
            String countryCode = receiver.getMaskedIp().split("\\.")[0];

            if (sender.getOriginalCountry().getCode().equalsIgnoreCase(countryCode)) {
                isPossible = true;
                return sender;
            } else {
                String countryName = "";
                if (countryCode.equalsIgnoreCase(CountryName.AUS.toCode())) {
                    countryName = CountryName.AUS.name();
                } else if (countryCode.equalsIgnoreCase(CountryName.CHI.toCode())) {
                    countryName = CountryName.CHI.name();
                } else if (countryCode.equalsIgnoreCase(CountryName.JPN.toCode())) {
                    countryName = CountryName.JPN.name();
                } else if (countryCode.equalsIgnoreCase(CountryName.USA.toCode())) {
                    countryName = CountryName.USA.name();
                } else {
                    countryName = CountryName.IND.name();
                }
                sender = connect(senderId, countryName);
                isPossible = true;
                return sender;
            }
        } else if (!receiver.getConnected()) {
            if (receiver.getOriginalCountry().getCode().equals(sender.getOriginalCountry().getCode())) {
                isPossible = true;
                return sender;
            } else {
                sender = connect(senderId, receiver.getOriginalCountry().getCountryName().name());
                isPossible = true;
                return sender;
            }
        } else {
            throw new Exception("Cannot establish communication");
        }
    }
}
