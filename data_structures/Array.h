//
// Created by chathura on 11/2/18.
//

#ifndef UNTITLED_ARRAY_H
#define UNTITLED_ARRAY_H
namespace harp {
    namespace ds {
        template<class TYPE>
        class Array {
        private:
            TYPE *data;
            int size;
            int start;

        public:
            Array(TYPE *data, int size, int start);

            TYPE *getData();

            int getSize();

            int getStart();

            void reset();
        };
    }
}
#endif //UNTITLED_ARRAY_H
